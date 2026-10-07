#!/usr/bin/env bash
# 从模板生成本地生效的 sharding.yaml（该文件已 gitignore，不会误提交密码）。
#
# 用法：
#   MYSQL_PASSWORD=你的密码 bash tools/app/gen-sharding-config.sh
# 可选环境变量：
#   SHARDING_DB_HOST（默认 127.0.0.1）  SHARDING_DB_PORT（默认 3306）
#   SHARDING_DB_USERNAME（默认 root）   DLYK_SHARDING_YAML（输出路径）
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${DLYK_SHARDING_YAML:-${ROOT_DIR}/dlyk-server/src/main/resources/sharding.yaml}"

HOST="${SHARDING_DB_HOST:-127.0.0.1}"
PORT="${SHARDING_DB_PORT:-3306}"
USERNAME="${SHARDING_DB_USERNAME:-root}"
PASSWORD="${MYSQL_PASSWORD:-}"

if [ -z "${MYSQL_PASSWORD:-}" ]; then
  echo "警告：MYSQL_PASSWORD 未设置，生成的配置里密码为空" >&2
fi

cat > "$OUT" <<EOF
# 本文件由 tools/app/gen-sharding-config.sh 生成（模板见 sharding.template.yaml），勿手改后提交。
mode:
  type: Standalone
  repository:
    type: Memory

databaseName: dlyk_sharding

dataSources:
  dlyk_ds:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    # 驱动模式的 dataSources 条目是扁平 Map：dataSourceClassName 与池参数同级，不嵌套 props
    url: jdbc:mysql://${HOST}:${PORT}/dlyk?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: ${USERNAME}
    password: "${PASSWORD}"
    maximumPoolSize: 10
    minimumIdle: 2

rules:
  # 单表（未分片的 17 张表）显式全量加载：默认懒加载在"分片表 JOIN 单表"的场景会报错
  - !SINGLE
    tables:
      - "*.*.*"
  - !SHARDING
    tables:
      t_clue_remark:
        actualDataNodes: dlyk_ds.t_clue_remark_\${0..3}
        tableStrategy:
          standard:
            shardingColumn: clue_id
            shardingAlgorithmName: t-clue-remark-inline
    shardingAlgorithms:
      t-clue-remark-inline:
        type: INLINE
        props:
          algorithm-expression: t_clue_remark_\${clue_id % 4}
          allow-range-query-with-inline-sharding: true
EOF

echo "已生成 $OUT"
echo "启动：DLYK_SHARDING_YAML=$OUT java -jar dlyk-server.jar --spring.profiles.active=sharding"
