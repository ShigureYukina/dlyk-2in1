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
| `dlyk-server/` | 后端：Spring Boot + MyBatis（原生 XML）+ Redis/Redisson + RabbitMQ + XXL-Job + Elasticsearch 检索，18 张表 / 19 个 Mapper |
| `dlyk-front/` | 前端：Vue 3 |
| `deploy/` | Nginx 反向代理（统一入口 / IP 限流 / gzip）与 Prometheus 配置 |
| `docker-compose.yml` | 本地依赖栈：MySQL / Redis / RabbitMQ / Elasticsearch / Nginx / XXL-Job Admin |

## 治理线速览

| 主题 | 做法 | 结果（本机实测口径） |
| --- | --- | --- |
| 慢查询治理 | PageHelper 联查 count 改单表统计（等价性前提：LEFT JOIN 全挂被驱动表主键）；深分页 `OFFSET` 改游标 | 单条 count 505/978 → 40/28ms；接口 P50 720 → 16ms |
| 缓存设计 | 列表 total 固定 key + 10s 短 TTL（只缓 total 不缓结果集），自埋点命中率指标 | 命中率 98.8%，列表 P50 250 → 19ms |
| 并发一致性 | Redisson 锁按线索串行化（锁在事务外）+ 条件唯一索引兜底 + 幂等切面 | 20 线程并发用例零重复 |
| 调度迁移 | `@Scheduled` → XXL-Job 分片广播，失败重试 / 告警平台侧配置 | 消除多实例重复执行 |
| 消息异步化 | 客户转换事件走 RabbitMQ（持久化 + 消费重试 + 开关回退），不阻塞主流程 | — |
| 检索补齐 | 线索关键字检索接 ES（副本字段最小化，命中后回 MySQL 拼详情），事件增量同步 + reindex 全量兜底 + 查询异常回落 | —（功能上线，未纳入压测口径） |
| 分表演练 | 跟进记录表按 clue_id 分 4 片（ShardingSphere-JDBC 驱动模式），分段 AUTO_INCREMENT 保 int 兼容 | —（机制演练 + 容量预案，未纳入压测口径） |
| 全局效果 | 全部缓存开关开启，按业务请求归一化 | 每千业务请求 DB 查询 1,203 → 602（-50%） |

> 口径四要素：同一份数据 / 同一台机器 / 同一轮 / 同一口径。测量方式与 baseline 的原始记录
> 为本机存档，未随仓库分发。

## 快速开始

```bash
docker compose up -d        # 一键起全栈：依赖栈 + 应用镜像（dlyk-server/Dockerfile 多阶段构建）
# 前置：.env 里配置 MYSQL_ROOT_PASSWORD 与 JWT_SECRET（缺失时 compose 直接报错）
# 导入 dlyk.sql（含 deleted 列、条件唯一索引兜底与对账表）
# 造数与压测脚本为本机存档，未随仓库分发

# 只起依赖栈、应用跑在宿主机的旧模式：
#   docker compose up -d mysql redis rabbitmq nginx
#   （deploy/nginx/nginx.conf 的 upstream 需改回 host.docker.internal:8089，文件内有注释）
```

### 环境变量（仓库内不落任何密钥）

| 变量 | 用途 | 配在哪 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | 依赖栈 MySQL root 密码 | `.env`（compose 自动读取，缺失时 compose 直接报错） |
| `MYSQL_PASSWORD` | 应用连接 MySQL 的密码 | 启动应用的环境（IDE 运行配置 / shell） |
| `JWT_SECRET` | JWT 签名密钥，缺失时应用拒绝启动 | 同上 |
| `DLYK_ES_ENABLED` | 线索 ES 检索开关（默认 false；开启后需 `docker compose up -d elasticsearch`） | 启动应用的环境 |
| `ES_URIS` | Elasticsearch 连接地址（默认 `http://127.0.0.1:9200`） | 同上 |

```bash
# 依赖栈密钥写进 .env（该文件已被 gitignore，不入库）：
#   MYSQL_ROOT_PASSWORD=你的密码
#   JWT_SECRET=一段足够长的随机串（如 openssl rand -base64 48 生成）
# 应用侧（宿主机直跑 jar/IDE）需要：
#   export MYSQL_PASSWORD=你的密码
#   export JWT_SECRET=同上
```

> 历史提交中曾出现旧的 DB 密码与 JWT 密钥（均已从代码移除并作废）。本机库仍在用旧密码的，
> 建议执行 `ALTER USER 'root'@'%' IDENTIFIED BY '<新密码>';` 轮换一次。

### 管理面

actuator 迁移到独立管理端口 `9090`（`management.server.port`），业务端口 8089 不再暴露任何
管理端点；Prometheus 按 `deploy/prometheus/prometheus.yml` 抓取 9090。缓存管理接口
（`/api/cache/*` 的写操作）限定 `admin` 角色。

## 说明

- 本仓库仅用于学习、复盘与求职展示；对账模块的规则设计不在本仓库范围内（本仓库负责调度、消息与接口对接）。
- 复刻底座的来源为公开教学课程项目，业务改造与治理线为本人在复刻环境中的实践记录。
