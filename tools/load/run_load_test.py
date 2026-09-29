"""
压测编排脚本

一次运行同时采集三类指标，避免"只看吞吐不看代价"：
    1. 接口延迟分位（Locust CSV）：P50 / P95 / P99
    2. 列表 total 缓存命中率（自埋点指标 dlyk_list_total_cache_gets_total）
    3. 数据库查询量（MySQL 全局状态）：Com_select 增量，归一化为"每千次请求的 DB 查询数"

关于分母：bypass 模式会在业务请求前额外发 DELETE /api/cache/clear 清缓存，
这类请求不产生 DB 查询。若把它们计入分母，"无缓存"一组的每千次请求 DB 查询数会被
稀释、显得更优。因此下面同时给出两个口径，并在报告里以"业务请求"口径为准。

注：Actuator 原生的 cache_gets_total 在本项目恒为 0 —— 用的是自定义 CacheManager Bean，
不会自增；所以命中率改由业务代码自己埋点。

用法：
    python run_load_test.py --mode cached --label 有缓存 --out docs/load/cached.md
    python run_load_test.py --mode bypass --label 无缓存 --out docs/load/bypass.md

依赖：locust、pymysql（见 requirements.txt）
"""

import argparse
import csv
import os
import socket
import subprocess
import sys
import threading
import time
import urllib.request

import pymysql

APP_HOST = os.getenv("DLYK_APP_HOST", "http://localhost:8089")
DB = dict(host=os.getenv("DLYK_DB_HOST", "127.0.0.1"),
          port=int(os.getenv("DLYK_DB_PORT", "3306")),
          user=os.getenv("DLYK_DB_USER", "root"),
          password=os.getenv("DLYK_DB_PASSWORD", "1056398086"),
          database="dlyk")

# 自埋点：列表 total 缓存的命中/未命中（见 com.dlyk.cache.ListTotalCache）
TOTAL_CACHE_COUNTER = "dlyk_list_total_cache_gets_total"

# 清缓存请求不算业务请求，从分母里剔除
NON_BUSINESS_MARK = "cache/clear"


def fetch_prometheus():
    """拉取 Prometheus 文本格式指标，返回原始文本"""
    with urllib.request.urlopen(f"{APP_HOST}/actuator/prometheus", timeout=10) as resp:
        return resp.read().decode("utf-8")


def counter_hit_miss(text, counter):
    """从 Prometheus 文本里取出指定 counter 按 result 标签拆分的 hit / miss"""
    hit = miss = 0
    for line in text.splitlines():
        if not line.startswith(counter) or line.startswith("#"):
            continue
        value = float(line.rsplit(" ", 1)[1])
        if 'result="hit"' in line:
            hit += value
        elif 'result="miss"' in line:
            miss += value
    return hit, miss


def redis_keyspace_stats():
    """
    读取 Redis 的键命中/未命中计数。

    注意：这统计的是**所有** Redis 键查找，除业务缓存外还包含登录态校验、
    分布式锁、幂等键的查找，因此它是命中率的近似上界，不等于缓存命中率本身。
    缓存效果的主证据是 A/B 对比下的 DB 查询量差异。
    """
    import socket
    with socket.create_connection((os.getenv("DLYK_REDIS_HOST", "127.0.0.1"),
                                   int(os.getenv("DLYK_REDIS_PORT", "6379"))), timeout=5) as sock:
        sock.sendall(b"INFO stats\r\n")
        payload = sock.recv(65536).decode("utf-8", errors="ignore")
    hits = misses = 0
    for line in payload.splitlines():
        if line.startswith("keyspace_hits:"):
            hits = int(line.split(":")[1])
        elif line.startswith("keyspace_misses:"):
            misses = int(line.split(":")[1])
    return hits, misses


# ListTotalCache 的 key 与三张被统计的表（见 com.dlyk.cache.ListTotalCache）
TOTAL_CACHE_KEYS = ["dlyk:list:total:t_clue", "dlyk:list:total:t_customer", "dlyk:list:total:t_tran"]


def resp_command(*args):
    """把命令编码成 RESP 数组，省一个 redis 客户端依赖"""
    parts = [b"*%d\r\n" % len(args)]
    for arg in args:
        payload = arg.encode("utf-8")
        parts.append(b"$%d\r\n%s\r\n" % (len(payload), payload))
    return b"".join(parts)


def invalidate_total_cache(stop_event):
    """
    注意：这是早期的对照手段，已被 dlyk.list-total-cache.enabled 开关取代。
    删除与写入存在竞态，实测命中率只能压到约 50%，做不到"缓存不存在"。
    保留是因为它不需要重启应用，粗测够用。
    """
    """
    后台线程持续删除列表 total 缓存。

    为什么要单独做这个对照，而不是复用 bypass 模式的 DELETE /api/cache/clear：
    Spring 的 RedisCache.clear() 默认走 KEYS 策略，bypass 模式下每清一次缓存
    都会在 Redis 上扫一遍 keyspace。上千次 KEYS 会拖慢整轮压测，
    让"无缓存"那组的接口延迟虚高，跨模式比延迟就不成立了。
    这里改成压测机直连 Redis 删 3 个固定 key（一次 DEL，O(1)），
    不加任何 HTTP 请求、不改变请求配比，从而得到干净的 A/B。
    """
    redis_host = os.getenv("DLYK_REDIS_HOST", "127.0.0.1")
    redis_port = int(os.getenv("DLYK_REDIS_PORT", "6379"))
    conn = socket.create_connection((redis_host, redis_port), timeout=5)
    try:
        while not stop_event.is_set():
            conn.sendall(resp_command("DEL", *TOTAL_CACHE_KEYS))
            deleted = conn.recv(64)
            if deleted.startswith(b"-"):
                raise RuntimeError(f"DEL 失败：{deleted!r}")
            time.sleep(0.05)
    finally:
        conn.close()


def db_query_count():
    """读取 MySQL 累计 SELECT 次数"""
    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:
            cur.execute("SHOW GLOBAL STATUS LIKE 'Com_select'")
            return int(cur.fetchone()[1])
    finally:
        conn.close()


def run_locust(mode, users, spawn_rate, duration, csv_prefix):
    env = dict(os.environ, DLYK_LOAD_MODE=mode)
    cmd = [sys.executable, "-m", "locust", "-f", "locustfile.py",
           "--host", APP_HOST, "--headless",
           "-u", str(users), "-r", str(spawn_rate), "-t", duration,
           "--csv", csv_prefix, "--only-summary"]
    print(">>> " + " ".join(cmd), file=sys.stderr)
    subprocess.run(cmd, env=env, cwd=os.path.dirname(os.path.abspath(__file__)), check=True)


def read_stats(csv_prefix):
    path = f"{csv_prefix}_stats.csv"
    with open(path, newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["cached", "bypass"], default="cached")
    parser.add_argument("--label", default="")
    parser.add_argument("--users", type=int, default=50)
    parser.add_argument("--spawn-rate", type=int, default=10)
    parser.add_argument("--duration", default="60s")
    parser.add_argument("--out", default="")
    parser.add_argument("--csv-suffix", default="", help="追加到 CSV 文件名，避免两轮对照互相覆盖")
    parser.add_argument("--invalidate-total-cache", action="store_true",
                        help="压测期间由本机直接删除列表 total 缓存，用于单独度量这层缓存的效果")
    args = parser.parse_args()

    # 加上后缀，避免和上一轮的 CSV 互相覆盖（对比时两轮都要留档）
    suffix = args.csv_suffix or ("_nototalcache" if args.invalidate_total_cache else "")
    csv_prefix = os.path.join(os.path.dirname(os.path.abspath(__file__)), f"result_{args.mode}{suffix}")

    hit_before, miss_before = counter_hit_miss(fetch_prometheus(), TOTAL_CACHE_COUNTER)
    db_before = db_query_count()
    r_hits_before, r_misses_before = redis_keyspace_stats()

    stop_event = threading.Event()
    invalidation = None
    if args.invalidate_total_cache:
        invalidation = threading.Thread(target=invalidate_total_cache, args=(stop_event,), daemon=True)
        invalidation.start()
        print(">>> 已启动 total 缓存失效线程（每 50ms DEL 一次）", file=sys.stderr)

    try:
        run_locust(args.mode, args.users, args.spawn_rate, args.duration, csv_prefix)
    finally:
        stop_event.set()
        if invalidation:
            invalidation.join(timeout=5)

    hit_after, miss_after = counter_hit_miss(fetch_prometheus(), TOTAL_CACHE_COUNTER)
    db_after = db_query_count()
    r_hits_after, r_misses_after = redis_keyspace_stats()

    rows = read_stats(csv_prefix)
    aggregated = [r for r in rows if r["Name"] == "Aggregated"]
    total_requests = int(float(aggregated[0]["Request Count"])) if aggregated else 0
    total_failures = int(float(aggregated[0]["Failure Count"])) if aggregated else 0
    non_business = sum(int(float(r["Request Count"])) for r in rows
                       if NON_BUSINESS_MARK in r["Name"])
    business_requests = total_requests - non_business

    hit = hit_after - hit_before
    miss = miss_after - miss_before
    lookups = hit + miss
    hit_rate = (hit / lookups * 100) if lookups else 0.0
    db_queries = db_after - db_before
    per_1k = (db_queries / total_requests * 1000) if total_requests else 0.0
    per_1k_business = (db_queries / business_requests * 1000) if business_requests else 0.0

    lines = []
    lines.append(f"# 压测结果（{args.label or args.mode}）\n")
    lines.append(f"- 模式：`{args.mode}`，并发 {args.users}，持续 {args.duration}")
    if args.invalidate_total_cache:
        lines.append("- 附加条件：压测期间持续删除列表 total 缓存（等价于该层缓存不存在）")
    lines.append(f"- 总请求数：{total_requests}，失败：{total_failures}")
    lines.append(f"- 其中业务请求：{business_requests}（剔除 {non_business} 次清缓存请求）")
    lines.append(f"- DB 查询量：**{db_queries}** 次")
    lines.append(f"- 归一化：每千次**全部请求** {per_1k:.0f} 次 DB 查询；"
                 f"每千次**业务请求** {per_1k_business:.0f} 次（跨模式对比用这个口径）")
    r_hits = r_hits_after - r_hits_before
    r_misses = r_misses_after - r_misses_before
    r_total = r_hits + r_misses
    if r_total:
        lines.append(f"- Redis 键命中率：{r_hits / r_total * 100:.1f}%"
                     f"（hit={r_hits}, miss={r_misses}；含锁/幂等/登录态查找，仅供参考）")
    if lookups:
        lines.append(f"- 列表 total 缓存命中率：**{hit_rate:.1f}%**"
                     f"（hit={int(hit)}, miss={int(miss)}）")
    lines.append("")
    lines.append("| 接口 | 请求数 | 失败 | P50(ms) | P95(ms) | P99(ms) | 平均(ms) |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- |")
    for row in rows:
        if row["Name"] in ("Aggregated", "") or not row["Name"]:
            continue
        lines.append("| {} | {} | {} | {} | {} | {} | {} |".format(
            row["Name"], row["Request Count"], row["Failure Count"],
            row["50%"], row["95%"], row["99%"], row["Average Response Time"]))

    report = "\n".join(lines) + "\n"
    print(report)
    if args.out:
        os.makedirs(os.path.dirname(args.out), exist_ok=True)
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(report)
        print(f">>> 已写入 {args.out}", file=sys.stderr)


if __name__ == "__main__":
    main()
