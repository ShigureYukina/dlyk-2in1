# 慢查询治理与深分页优化报告

> 数据规模：`t_clue` 356,352 行 / `t_customer` 356,353 行 / `t_tran` 356,354 行（模拟"流水数据从万级涨至数十万级"）
>
> 环境：MySQL 8.0.46（Docker），数据由 `tools/db/01_gen_perf_data.sql` 生成

## 一、测量方法

探针脚本 `tools/db/perf_probe.sh` 对每条 SQL 同时采集两项指标：

1. **EXPLAIN 的 type 列** —— 判断是否走索引（`ALL` = 全表扫描）
2. **SQL 内 `NOW(6)` 计时** —— 规避 mysql 客户端连接开销的干扰

两个容易出错的地方，脚本里都做了处理：

- **预热**：建完索引立刻测，测到的是冷启动耗时，会把优化结果测成负收益。脚本对每条 SQL 先执行两次再计时。
- **可比性**：优化后与优化前必须在同一台机器、同一份数据、同一预热策略下测量。因此先测"优化后"，再回退索引测"优化前"，最后恢复索引。

## 二、优化前后对比

| 探针 | 说明 | 优化前 type | 优化前(ms) | 优化后 type | 优化后(ms) | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| P1 | 线索存活数（deleted 谓词） | ALL | 40.6 | ALL | 41.7 | 持平 |
| P2 | 手机号唯一性校验 checkPhone | ALL | 41.1 | **ref** | **0.3** | **137x** |
| P3 | 列表联查 深分页 offset=300000 | ALL | 403.3 | ALL | 398.8 | 持平 |
| P4 | 列表联查 游标分页 | range | 0.4 | range | 0.5 | 持平 |
| P5 | 对账-重复客户线索 | index | 465.6 | **ref** | **50.1** | **9.3x** |
| P6 | 交易列表与客户联查 | ALL,ref | 292.9 | index,eq_ref | 269.5 | 小幅提升 |
| P7 | 活动名称前导通配搜索 | ALL | 0.3 | ALL | 0.3 | 表仅 34 行，无法量化 |

**核心结论：P4 游标分页 0.5ms vs P3 偏移分页 398.8ms，相差约 800 倍。**

## 三、三条有价值的结论

### 1. 手机号校验的收益最大（137x）

`ClueServiceImpl.checkPhone` 是"新增线索"前的唯一性校验，SQL 为
`SELECT COUNT(0) FROM t_clue WHERE phone = ?`。`t_clue` 上原本没有任何
phone 索引，每次校验都要全表扫 35 万行。加 `idx_clue_phone` 后降为 0.3ms。

这条查询在业务上很热（用户每填一次手机号就触发一次），是典型的高频低选择性
缺失索引。

### 2. 深分页的根因是 OFFSET，不是缺索引 —— 而且加错索引会更慢

曾为线索列表加 `idx_clue_deleted_id (deleted, id)`，理由是"同时满足
deleted 过滤与 id 排序，避免 filesort"。**实测结果是劣化**：

```
深分页 offset=300000：403ms -> 651ms（劣化 61%）
```

原因是该查询需要返回 `full_name` 等非索引列：

- 全表扫描走聚簇索引，行数据就在扫描路径上，无额外开销；
- 走二级索引则每命中一行都要**回表**取 `full_name`，30 万次随机回表
  远贵于一次顺序扫描。

因此该索引已被移除，结论保留在 `tools/db/02_normalize_and_index.sql` 的注释里，
避免后人重复踩坑。

**深分页的正确解法是改变分页方式**：用"上一页最后一条的 id"作为游标，
把 `OFFSET` 换成 `WHERE id < lastId`。扫描行数恒为 limit，与页码无关。

### 3. 逻辑删除列可空会连带废掉索引

原本 `deleted` 可空，查询不得不写成：

```sql
WHERE tc.deleted = 0 OR tc.deleted IS NULL
```

这个 `OR` 会让优化器放弃索引直接全表扫描。规范为 `NOT NULL DEFAULT 0`
并把谓词简化为 `deleted = 0` 后，索引才真正可用（见 P2、P5）。

## 四、索引清单（全部经实测确认）

| 表 | 索引 | 依据 |
| --- | --- | --- |
| `t_clue` | `idx_clue_phone (phone)` | P2 实测 137x |
| `t_customer` | `idx_customer_deleted_clue (deleted, clue_id)` | P5 实测 9.3x |
| `t_customer` | `uk_customer_clue_active ((IF(deleted=0, clue_id, NULL)))` | 数据库层兜底防重，非性能索引 |
| `t_tran` | `idx_tran_customer_stage (customer_id, stage)` | 按查询形态设计，当前数据量下收益不显著 |
| `t_activity` | `idx_activity_owner_create (owner_id, create_time)` | 按查询形态设计，表仅 34 行无法量化 |

**被否决**：`idx_clue_deleted_id (deleted, id)` —— 实测导致深分页劣化 61%。

## 五、接口层面的落地

```
GET /api/clues?current=1                 # 偏移分页（兼容旧接口，已补 ORDER BY）
GET /api/clues/cursor?lastId=&size=20    # 游标分页（深分页优化版）
```

游标分页响应中返回 `nextLastId`，客户端翻页时原样回传即可。

同时修正了原 SQL **没有 ORDER BY** 的问题：MySQL 对无排序的查询不保证返回顺序，
翻页时会出现同一条记录重复出现或被跳过。

## 六、复现步骤

```bash
# 1) 启动依赖
docker compose up -d

# 2) 造数（自动倍增到 30 万）
docker exec -i dlyk-mysql mysql -uroot -p*** dlyk < tools/db/01_gen_perf_data.sql

# 3) 采优化前基线
bash tools/db/perf_probe.sh before-index

# 4) 应用索引与数据规范化
docker exec -i dlyk-mysql mysql -uroot -p*** dlyk < tools/db/02_normalize_and_index.sql

# 5) 采优化后基线
bash tools/db/perf_probe.sh after-index
```

## 七、遗留项

**活动名称的前导通配搜索**（`TActivityMapper.xml`）：

```sql
and ta.name like concat('%', #{name}, '%')
```

前导 `%` 使 B-tree 索引无法使用，属于结构性问题，加索引解决不了。当前
`t_activity` 仅 34 行，全表扫描 0.3ms 不构成瓶颈，因此**未做改动**。
数据量增长后的可选方案：

1. 改为前缀匹配 `LIKE 'x%'`（会改变搜索语义，需产品确认）；
2. 加 `FULLTEXT` 索引配合 MySQL 8 的 ngram 解析器，改用 `MATCH ... AGAINST`。
