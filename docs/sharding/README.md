# 分表演练：t_clue_remark（跟进记录）× ShardingSphere-JDBC

跟进记录是全库水位最高的流水表（perf 造数 35.6 万行的主承载表之一，也是简历口径里
「数十万级流水」的本体）。本篇记录用 ShardingSphere-JDBC 做的**分表（不分库）演练**：
方案、5.5.x 驱动模式的完整依赖清单、以及一个反直觉的主键设计决策。

## 方案速览

```
t_clue_remark ──按 clue_id 取模──> t_clue_remark_0 / _1 / _2 / _3

路由语义：
  insert / 按 clue_id 列表查询 ──> clue_id 等值分片，单片命中
  selectByPrimaryKey / update / delete（只带 id）──> 广播 4 张物理表后归并
  PageHelper 分页（带 clue_id）──> LIMIT 改写在单片内完成，无跨片归并
```

为什么分片键选 `clue_id`：跟进记录的全部业务读取都是"线索详情页里看跟进历史"，
即 `WHERE clue_id = ?`——等值分片键与主查询路径重合，是最教科书的选择。
代价是按 id 的单条操作要广播，这类操作在跟进记录上频率极低（后台管理动作）。

## 关键决策：主键不用 ShardingSphere 雪花

常见的分表方案是"雪花 ID + SS key-generate"。这里刻意不用：

| 约束 | 雪花 ID 的问题 | 本方案的解法 |
| --- | --- | --- |
| 实体/列类型 | 雪花是 64 位，`id` 列和实体都是 int（Integer），要么改列类型要么溢出 | 保持 int |
| 前端安全整数 | JS Number 只有 2^53，雪花要 Long→String 序列化改造 | id 全程 JS 安全 |
| 改造成本 | 实体/Mapper/前端/Excel 导出全链路改类型 | **业务代码零改动** |

替代方案是**分段 AUTO_INCREMENT**：4 张物理表各自自增，起点错开 5 亿
（1 / 5亿 / 10亿 / 15亿，int 上限 21.47 亿，每表 5 亿行余量 = 当前水位的 1000 倍以上），
保证跨片全局唯一。这是 MyCat 时代"步长错开"思路的变体：步长方案靠 `auto_increment_increment`
（全局变量，改了影响整个实例），分段起点只动各表自己的 `AUTO_INCREMENT`，无副作用。

诚实口径：**数十万级水位本来不需要分表**，这是容量预案演练——把"路由、归并、全局唯一 id"
的机制先跑通，等真到了量级，分库只是换数据源拓扑、配置结构不变。

## ShardingSphere 5.5.x 驱动模式的真实依赖清单

5.3+ 没有 spring-boot-starter，走 JDBC 驱动模式
（`jdbc:shardingsphere:absolutepath:...`）。**聚合包 `shardingsphere-jdbc` 5.5.3
远不是全部**，按首次启动的报错顺序补齐了这些模块：

| 报错 | 缺什么 |
| --- | --- |
| `No implementation ... ShardingSphereModeConfigurationURLLoader with type 'classpath:'` | `shardingsphere-infra-url-classpath` / `-url-absolutepath`（配置 URL 加载器拆成独立模块） |
| `Invalid tag: !SHARDING` | `shardingsphere-sharding-core`（聚合包不带功能规则） |
| `Cannot invoke Object.toString() ... Map.get null`（StorageUnit NPE） | `shardingsphere-infra-data-source-pool-hikari`（Hikari 池元数据 SPI，url↔jdbcUrl 同义词表在这里） |
| `No implementation ... ContextManagerBuilder with type 'null'` | `shardingsphere-standalone-mode-core` + `-repository-memory`（聚合包只带 cluster-mode） |
| `No implementation ... PrivilegeProvider with type 'ALL_PERMITTED'` | `shardingsphere-authority-simple` |
| `No implementation ... DialectSQLParserFacade` | `shardingsphere-parser-sql-engine-mysql`（MySQL 方言解析器） |
| `NoClassDefFoundError: org/apache/commons/lang3/Strings` | commons-lang3 钉到 3.18+（Boot 3.2 托管的是 3.13） |
| `MOD ... can not use auto sharding algorithm` | standard 策略配 INLINE 表达式算法（`t_clue_remark_${clue_id % 4}`），MOD/HASH_MOD 是 auto 策略专用 |
| 分片表 JOIN 单表报 `Table or view does not exist` | `!SINGLE rules` 显式 `tables: ["*.*.*"]` 全量加载单表（默认懒加载踩坑） |
| PageHelper 报 `must set the helper property` | 驱动模式 URL 推断不出方言，显式配 `pagehelper.helper-dialect: mysql` |

另外两个驱动模式的坑，都写进了 `sharding.template.yaml` 的注释：

- **驱动模式不替换 YAML 里的环境变量占位符**（与 Server 模式不同）。所以配置走
  「模板 `sharding.template.yaml`（入库）→ 生成脚本 `tools/app/gen-sharding-config.sh`（写入密码）
  → `sharding.yaml`（gitignore）→ `application-sharding.yml` 用 `absolutepath` 指向」的流程。
- **dataSources 条目是扁平 Map**：`dataSourceClassName` 与池参数同级，不嵌套 `props`——
  嵌套会让 StorageUnit 读不到 url/username，报出很难反推的 NPE。

## 复现步骤

```bash
# 1. 生成含真实密码的分片配置（生成物已 gitignore）
MYSQL_PASSWORD=你的密码 bash tools/app/gen-sharding-config.sh

# 2. 启动（或 IDE 配 DLYK_SHARDING_YAML + sharding profile）
DLYK_SHARDING_YAML=dlyk-server/src/main/resources/sharding.yaml \
java -jar dlyk-server.jar --spring.profiles.active=sharding

# 3. 存量数据搬迁（可选）：tools/db/03_sharding_setup.sql
```

测试：`ClueRemarkShardingIntegrationTest` 端到端覆盖（物理表落位、跨片 id 唯一、
主键广播、PageHelper 分页）。注意物理表必须先于 Spring 上下文创建——SS 在启动期做
元数据加载，测试里用 `@DynamicPropertySource` 在装配前生成配置并建表。

## 面试挂靠要点

- 分片键选择 = 把"最高频查询条件"和"分片键"对齐，而不是拍脑袋 hash 主键。
- 雪花 ID 不是免费午餐：类型升级（int→bigint）、前端 JS 安全整数、全链路改造都是隐性成本；
  分段自增是低成技术债可控的替代（上限 = 段宽，要算余量）。
- PageHelper + SS 的 LIMIT 改写：等值分片键查询单片命中，无深分页放大；跨片深分页
  （`limit offset,size`）会被改写为每个分片取 offset+size 再归并，offset 越大浪费越多——
  这正是 CRM 主链路早已改游标分页的原因（`docs/perf`）。
- 「为什么 35 万行就分表？」——如实说：不需要，这是机制演练 + 容量预案，分库的增量
  只在数据源拓扑，配置结构不变。
