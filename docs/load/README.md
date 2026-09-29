# 压测报告

> 工具：Locust 2.46.5（脚本 `tools/load/locustfile.py`，编排 `tools/load/run_load_test.py`）
> 另有 JMeter 5.6.3 交叉验证，见 `docs/load/jmeter.md`（结论一致；绝对延迟不可跨工具比较，原因见该文末节）
> 配置：50 并发 / 60 秒 / 0 失败
> 数据量：`t_clue` 35.6 万、`t_customer` 35.6 万、`t_tran` 35.6 万
> 应用：宿主 8089（`XXL_JOB_ENABLED=false`，避免定时任务污染统计），MySQL / Redis / RabbitMQ 在 Docker

## 一、结论速览

| # | 结论 | 数据 |
| --- | --- | --- |
| 1 | 全局 DB 查询量下降约 **50%** | 每千业务请求 1,203 → 602 |
| 2 | 列表接口 P50 从 **250ms 降到 19ms** | 去掉每页 COUNT 的直接收益 |
| 3 | 列表 total 缓存命中率 **98.8%** | 自埋点 `dlyk_list_total_cache_gets_total` |
| 4 | 深分页是游标分页的 **45 倍** | P50 720ms vs 16ms（同一轮内对比） |
| 5 | 热点接口缓存**完全拦截**数据库 | 活动列表 / 字典值 DB 查询 0 次 |

**"DB 查询量下降 70%" 未达成，实测约 50%。** 见第五节。

## 二、测量方法

| 指标 | 采集方式 |
| --- | --- |
| 接口延迟分位 | Locust 导出的 CSV（P50/P95/P99） |
| DB 查询量 | MySQL `SHOW GLOBAL STATUS LIKE 'Com_select'` 的窗口增量 |
| 列表 total 缓存命中率 | 业务代码自埋点，Prometheus 拉取 |

### 坑一：分母污染

"无缓存"场景靠每次请求前调 `DELETE /api/cache/clear` 模拟，这类请求不访问数据库。
直接拿总请求数当分母，会算出"有缓存反而更费 DB"的错误结论。因此统一按**业务请求**
（剔除清缓存请求）归一化。

### 坑二：清缓存本身会拖慢整轮压测

Spring 的 `RedisCache.clear()` 默认走 KEYS 策略，bypass 模式下每清一次缓存都会在
Redis 上扫一遍 keyspace。上万次 KEYS 会把"无缓存"那组所有接口的延迟一起抬高，
**跨模式比延迟不成立**。

所以延迟的 A/B 换了一个干净的对照（见第四节）：同样是 `cached` 模式，只把
列表 total 缓存用开关旁路（`LIST_TOTAL_CACHE_ENABLED=false`）后重启应用，
两轮请求配比完全一致、没有任何多余请求。

## 三、全局对比：DB 查询量下降约 50%

| 场景 | 业务请求数 | DB 查询数 | 每千业务请求的 DB 查询 | 相对基线 |
| --- | --- | --- | --- | --- |
| A 全部缓存关闭（bypass） | 14,676 | 17,656 | **1,203** | 基线 |
| B 全部缓存开启 | 24,350 | 14,664 | **602** | **-50.0%** |

```
(1203 - 602) / 1203 = 49.96%
```

三轮压测的请求配比经核对基本一致（活动 22~23%、字典 19%、四个列表/校验接口 48~49%），
因此该对比成立。

### 热点接口是"完全拦截"

| 接口 | 无缓存时 DB 查询 | 有缓存时 DB 查询 | 下降 |
| --- | --- | --- | --- |
| 活动列表 `/api/activitys` | 约 3,200 | 0 | **100%** |
| 字典值 `/api/dicvalue/{typeCode}` | 约 2,750 | 0 | **100%** |

这两个接口是前端每次渲染页面都会调用的高频接口，缓存收益最直接。

### 为什么剩下的降不下去

有缓存那轮 602 次/千请求，几乎全部是"一次请求换一次查询"，无法靠缓存消除：

| 来源 | 每千请求的 DB 查询 | 能否缓存 |
| --- | --- | --- |
| 线索列表 / 客户列表 / 深分页（结果集） | 约 340 | 缓存则列表有秒级延迟，需产品确认 |
| 线索游标分页（结果集） | 约 150 | 同上 |
| 手机号唯一性校验 | 约 95 | **不能**，校验必须实时 |
| 交易列表 | 约 10 | — |
| 活动 / 字典 | 0 | 已缓存 |

## 四、干净 A/B：列表 total 缓存单独贡献 37%

两轮都是 `cached` 模式，唯一差别是 total 缓存开关：

| 场景 | 业务请求数 | DB 查询数 | 每千业务请求的 DB 查询 | 列表 total 缓存命中率 |
| --- | --- | --- | --- | --- |
| total 缓存关闭 | 13,978 | 13,365 | **956** | — |
| total 缓存开启 | 24,350 | 14,664 | **602** | 98.8% |

```
这层缓存单独贡献：(956 - 602) / 956 = 37.0%
```

延迟同步改善（同模式、无额外请求，可直接对比）：

| 接口 | 关闭 P50 / P99 (ms) | 开启 P50 / P99 (ms) |
| --- | --- | --- |
| `/api/clues?current=1` | 250 / 550 | **19 / 190** |
| `/api/customers?current=1` | 250 / 550 | **19 / 210** |
| `/api/clues/cursor` | 57 / 210 | 16 / 150 |
| `/api/clue/{phone}` | 57 / 180 | 14 / 140 |
| `/api/activitys` | 8 / 100 | 7 / 39 |
| `/api/dicvalue/{typeCode}` | 6 / 78 | 4 / 26 |

**反直觉的一点**：单表 `COUNT(*)` 单独跑只有 40ms，看起来无足轻重；但在 50 并发下
它才是列表接口的主要成本——P50 差了 230ms。原因是这个 count 每页都要执行、
且要在 35.6 万行上扫描，高并发时和分页查询争抢缓冲池与 CPU。
**"单条 SQL 快"不等于"这个查询模式可以每页都打一次"。**

## 五、深分页：同一轮压测内的对比

| 接口 | P50(ms) | P95(ms) | P99(ms) |
| --- | --- | --- | --- |
| `/api/clues/cursor?size=20`（游标） | **16** | 100 | **150** |
| `/api/clues?current=1`（偏移-浅页） | 19 | 110 | 190 |
| `/api/clues?current=5000`（偏移-深页） | **720** | 1,200 | **1,400** |

深页比游标慢约 **45 倍（P50）**，且随偏移量线性恶化——游标分页的扫描行数恒为 limit，
与页码无关。

## 六、本轮修复的瓶颈：PageHelper 对 7~9 表联查自动 count

压测前列表接口 P50 高达 1000~1900ms。根因是 PageHelper 自动为原 SQL 生成 count，
而列表 SQL 带 7~9 个 LEFT JOIN：

| 统计方式 | 线索列表 | 客户列表 |
| --- | --- | --- |
| PageHelper 对联查 SQL 做 count | 505.8ms | 977.8ms |
| 单表 count | **40.2ms** | **27.9ms** |
| 提升 | 12.6x | 35x |

**为什么可以换成单表 count**：所有 LEFT JOIN 都挂在被驱动表的主键上
（`t_user.id`、`t_dic_value.id`、`t_product.id`），至多匹配一行，不会放大结果集，
因此 `SELECT COUNT(*) FROM t_clue WHERE deleted = 0` 与联查统计等价。
已验证与数据库直连统计完全一致（线索 356,333、客户 356,344）。

改动：`PageHelper.startPage(current, size, false)` 跳过自动 count，total 改由单表统计提供。

**带筛选条件的交易列表保留 PageHelper 自动 count**：筛选条件可能落在被关联的表上，
单表统计会算错 total。正确性优先于性能。

## 七、未达成项

**"DB 查询量下降 70%" 未达成，实测约 50%。** 剩余部分要压到 70% 只能缓存列表结果集：

- **代价**：列表页存在秒级数据延迟（产品可感知），且缓存键需按"页码 + 筛选条件"构造，
  键数量随筛选组合膨胀。
- **判断**：CRM 的列表页是业务员做跟进决策的入口，秒级脏数据会直接影响判断，
  属于产品决策而非技术决策，**本轮不擅自改动**，按实测值如实表述。

其它可选项：改造为 ES / 读写分离，适合数据量继续增长后做查询与统计分离，改动最大。

## 八、复现步骤

```bash
# 1. 停 XXL-Job 调度，否则对账任务会在统计窗口内产生数十万次 Com_select
docker exec dlyk-mysql mysql -uroot -p*** -e "UPDATE xxl_job.xxl_job_info SET trigger_status = 0;"

# 2. 以压测配置重启应用（停进程 → 打包 → 启动，Windows 上运行中的 jar 会被锁住）
cd <repo>
bash tools/app/restart-app.sh false --build

# 3. 三轮压测
cd tools/load
pip install -r requirements.txt
python run_load_test.py --mode bypass --csv-suffix _bypass --label "全部缓存关闭" \
    --users 50 --spawn-rate 10 --duration 60s --out ../../docs/load/bypass.md
python run_load_test.py --mode cached --csv-suffix _on --label "全部缓存开启" \
    --users 50 --spawn-rate 10 --duration 60s --out ../../docs/load/cached.md

# 4. total 缓存单独对照（需重启应用旁路该层缓存）
export LIST_TOTAL_CACHE_ENABLED=false && bash tools/app/restart-app.sh false
python run_load_test.py --mode cached --csv-suffix _off --label "total 缓存关闭" \
    --users 50 --spawn-rate 10 --duration 60s --out ../../docs/load/cached-totalcache-off.md
```

```bash
# 5. JMeter 交叉验证（结论一致，用来确认统计口径没问题）
bash tools/jmeter/run.sh 50 60
```

原始 CSV 见 `tools/load/result_*_stats.csv`；JMeter 原始样本见 `tools/jmeter/result.jtl`。
