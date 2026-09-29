# dlyk-2in1 · CRM 治理线实践环境

> **这是什么**：我在实习期间（2025.06–2026.05）搭建的本地实践环境——基于一套公开教学课程的
> CRM 项目复刻底座（原包名 `com.bjpowernode`，已重构为 `com.dlyk`），在其上按真实业务场景
> 完成了一轮「治理线」改造：慢查询治理、缓存设计、并发一致性、调度迁移、消息异步化，
> 并配套了完整的压测与性能排查文档。
>
> **时间线如实说明**：2025-08 起在复刻底座上做业务功能开发；治理线代码与压测文档在实习结束
> 后（2026-09）整理入库。所有压测数据均为**本机复刻环境实测**（Docker MySQL/Redis/RabbitMQ +
> 造数脚本），不代表任何生产系统。

## 模块结构

| 目录 | 内容 |
| --- | --- |
| `dlyk-server/` | 后端：Spring Boot + MyBatis（原生 XML）+ Redis/Redisson + RabbitMQ + XXL-Job，18 张表 / 19 个 Mapper |
| `dlyk-front/` | 前端：Vue 3 |
| `docs/load/` | 压测记录：Locust / JMeter、干净 A/B 对照、口径说明 |
| `docs/perf/` | 性能排查：慢 SQL 探针、索引实验、Redis 往返统计口径 |
| `tools/` | Locust 场景脚本、JMeter 测试计划、DB 造数与探针脚本、重启脚本 |
| `deploy/` | Nginx 反向代理（统一入口 / IP 限流 / gzip）与 Prometheus 配置 |
| `docker-compose.yml` | 本地依赖栈：MySQL / Redis / RabbitMQ / Nginx / XXL-Job Admin |

## 治理线速览（细节见 docs/）

| 主题 | 做法 | 结果（本机实测口径） |
| --- | --- | --- |
| 慢查询治理 | PageHelper 联查 count 改单表统计（等价性前提：LEFT JOIN 全挂被驱动表主键）；深分页 `OFFSET` 改游标 | 单条 count 505/978 → 40/28ms；接口 P50 720 → 16ms |
| 缓存设计 | 列表 total 固定 key + 10s 短 TTL（只缓 total 不缓结果集），自埋点命中率指标 | 命中率 98.8%，列表 P50 250 → 19ms |
| 并发一致性 | Redisson 锁按线索串行化（锁在事务外）+ 条件唯一索引兜底 + 幂等切面 | 20 线程并发用例零重复 |
| 调度迁移 | `@Scheduled` → XXL-Job 分片广播，失败重试 / 告警平台侧配置 | 消除多实例重复执行 |
| 消息异步化 | 客户转换事件走 RabbitMQ（持久化 + 消费重试 + 开关回退），不阻塞主流程 | — |
| 全局效果 | 全部缓存开关开启，按业务请求归一化 | 每千业务请求 DB 查询 1,203 → 602（-50%） |

> 口径四要素：同一份数据 / 同一台机器 / 同一轮 / 同一口径。测量方式与 baseline 逐项见
> `docs/load/README.md` 与 `docs/perf/README.md`。

## 快速开始

```bash
docker compose up -d        # 起依赖栈（MySQL/Redis/RabbitMQ/Nginx/XXL-Job Admin）
# 导入 dlyk.sql（含 deleted 列与升级脚本 sql/upgrade/）
# 造数：tools/db/01_gen_perf_data.sql（35.6 万行压测数据）
# 压测：tools/load/locustfile.py（Locust 2.46.5，50 并发 / 60s）
```

## 说明

- 本仓库仅用于学习、复盘与求职展示；对账模块的规则设计不在本仓库范围内（本仓库负责调度、消息与接口对接）。
- 复刻底座的来源为公开教学课程项目，业务改造、治理线与全部文档为本人在复刻环境中的实践记录。
