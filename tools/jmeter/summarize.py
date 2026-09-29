"""
汇总 JMeter 结果（result.jtl）为 Markdown 报表。

JTL 是 CSV，这里不依赖 pandas，避免为一次汇总装依赖。

分位取法：把 elapsed 升序后取最近秩，与 Locust 内部使用的
分位算法不完全相同，因此两套工具的差值应以量级为准，不必逐毫秒对齐。

用法：
    python tools/jmeter/summarize.py tools/jmeter/result.jtl docs/load/jmeter.md
"""

import csv
import sys
from collections import defaultdict


def percentile(sorted_values, ratio):
    if not sorted_values:
        return 0
    index = int(round(ratio * (len(sorted_values) - 1)))
    return sorted_values[index]


def main():
    jtl_path = sys.argv[1] if len(sys.argv) > 1 else "result.jtl"
    out_path = sys.argv[2] if len(sys.argv) > 2 else ""

    per_label = defaultdict(list)
    errors = defaultdict(int)
    start = end = None

    with open(jtl_path, newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            label = row["label"]
            per_label[label].append(int(row["elapsed"]))
            if row["success"] != "true":
                errors[label] += 1
            ts = int(row["timeStamp"])
            start = ts if start is None else min(start, ts)
            end = ts + int(row["elapsed"]) if end is None else max(end, ts + int(row["elapsed"]))

    total = sum(len(v) for v in per_label.values())
    total_errors = sum(errors.values())
    duration = (end - start) / 1000 if start else 0

    lines = []
    lines.append("# JMeter 压测结果\n")
    lines.append(f"- 总样本数：{total}，失败：{total_errors}")
    lines.append(f"- 时长：{duration:.1f}s，吞吐：{total / duration:.1f} 请求/秒" if duration else "")
    lines.append("")
    lines.append("| 接口 | 请求数 | 失败 | 平均(ms) | P50 | P95 | P99 | 最大 |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- | --- |")

    # 按请求数降序，登录这种一次性的排在后面
    for label, values in sorted(per_label.items(), key=lambda kv: -len(kv[1])):
        values.sort()
        avg = sum(values) / len(values)
        lines.append("| {} | {} | {} | {:.1f} | {} | {} | {} | {} |".format(
            label, len(values), errors[label], avg,
            percentile(values, 0.50), percentile(values, 0.95),
            percentile(values, 0.99), values[-1]))

    all_values = sorted(v for values in per_label.values() for v in values)
    if all_values:
        lines.append("| **合计** | {} | {} | {:.1f} | {} | {} | {} | {} |".format(
            len(all_values), total_errors, sum(all_values) / len(all_values),
            percentile(all_values, 0.50), percentile(all_values, 0.95),
            percentile(all_values, 0.99), all_values[-1]))

    report = "\n".join(line for line in lines if line is not None) + "\n"
    print(report)
    if out_path:
        with open(out_path, "w", encoding="utf-8") as handle:
            handle.write(report)
        print(f">>> 已写入 {out_path}", file=sys.stderr)


if __name__ == "__main__":
    main()
