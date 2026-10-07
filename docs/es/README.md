# 线索检索：MySQL + Elasticsearch

线索列表的关键字检索从「无」到「ES」：历史上前端搜索框一直在发 `name`/`phone` 参数，后端 `/api/clues` 只绑了 `current` 直接丢弃（见 `perf/README.md` 遗留项里的前缀 LIKE 问题——其实当时连 LIKE 都没有）。本篇记录接入方案、一致性模型与取舍。

## 方案速览

```
写路径（MySQL 为主库，ES 为检索副本）
  新增/编辑线索 ─┐
  编辑线索      ─┼─> clue.changed ─┐
  逻辑删除      ─┘   clue.deleted ─┴─> dlyk.es.sync.queue ─> EsSyncConsumer ─> ES 文档
  转化为客户（改 t_clue.state）─> clue.changed ─┘

读路径
  GET /api/clues?name=&phone=
    ├─ dlyk.es.enabled=false 或无关键字 → 原 MySQL 分页路径（缓存 + PageHelper）
    └─ 有关键字 → ES bool 查询（fullName ngram 短语匹配 / phone 前缀 + deleted=0 过滤）
         └─ 命中 id 按 ES 顺序回 MySQL selectCluesByIds（7 表 JOIN 详情）→ 与原接口同构的 PageInfo

兜底
  POST /api/es/reindex（admin）→ 删索引重建映射 → 游标分页全量扫 t_clue → bulk 索引
  ES 查询异常 → 本次请求回落 MySQL 列表路径（warn 留痕），搜索是增强不是依赖
```

设计上与治理线的对账同构：**实时增量（事件）+ 全量兜底（reindex）+ 降级开关（dlyk.es.enabled）**。

## 关键取舍

| 决策 | 为什么 |
|---|---|
| ES 只存 id/fullName/phone/ownerName/deleted 五个字段 | 列表详情的 7 张关联表不冗余进索引，否则"字典改名要不要刷索引"这类一致性问题全要自己扛；命中后回 MySQL 拼装，前端无感 |
| ngram tokenizer（min 1 / max 10）而不是 ik 分词 | ngram 是 ES 内置能力，容器不用装插件，CI 才能起真实 ES 跑集成测试；代价是索引体积更大、超过 10 字的连续子串搜不到。中文生产场景对词库要求高时再上 ik |
| 同步走 MQ 事件而不是双写 | 与既有 `customer.converted` 事件同一套基础设施（topic exchange + Jackson2JsonMessageConverter）；发布方吞异常，ES 抖动不阻塞线索新增 |
| 动作用路由键区分而不是事件体字段 | 队列只有一条，事件体只带 clueId，"文档长什么样"由消费端查库决定 |
| 查询异常回落而不是报错 | 列表页 500 比"忽略关键字"更伤；但回落必须 warn 留痕，不能静默 |
| deleted=0 过滤放 filter | 不算分、可缓存；逻辑删除在消费端还会直接删文档，双保险 |

## 一致性窗口

事件同步是最终一致：发布失败/消费失败（重试 3 次后丢弃）/ES 写入失败都会留下缺口，直到下一次 reindex 补齐。这个窗口与对账线（`docs/perf` 与治理线 README 的对账叙事）接受的是同一类窗口，只是对象从数据库行换成了搜索文档。

## 复现步骤

```bash
# 1. 起依赖（含 elasticsearch，8.11.4 单机、关安全认证、512m 堆）
docker compose up -d elasticsearch

# 2. 应用开启检索开关启动
DLYK_ES_ENABLED=true mvn spring-boot:run   # 或在 IDE 里配该环境变量

# 3. 全量重建索引（种子数据 + 造数均可，tools/db/01_gen_perf_data.sql 可造 35.6 万行）
curl -X POST http://localhost:8089/api/es/reindex   # 需 admin 角色

# 4. 搜索（前端搜索框本来就在发这两个参数）
curl "http://localhost:8089/api/clues?current=1&name=测试&phone=139"
```

测试：
- `ClueSearchServiceTest`：排序/过滤/开关判断的纯单测，不依赖 ES。
- `ClueEsSearchIntegrationTest`：真实 ES 端到端（重建→索引→检索→删除），靠 `DLYK_ES_IT=true` 门控——本地没有 ES 自动跳过，CI 的 elasticsearch service 容器里真跑。

## 面试挂靠要点

- **为什么不用 MySQL FULLTEXT/LIKE**：LIKE '%x%' 前缀通配吃不到索引（perf/README 遗留项）；FULLTEXT 对中文要 ngram parser 且查询能力/扩展性都不如 ES，也和"检索层独立扩容"的方向不符。
- **一致性**：实时增量 + 全量兜底 + 接受窗口，与对账兜底同一套方法论；能说清"什么时候会不一致、多久能修复、修复入口在哪"。
- **回表拼装**：ES 返回 id → MySQL 补详情，是搜索系统的标准分层（索引层做匹配、主库做事实），顺带避免索引冗余字段的更新风暴。
- **ngram vs ik**：前者内置可 CI、子串语义准、索引大；后者词库语义好、索引小、要装插件。选择取决于部署约束而不是哪个"更好"。
