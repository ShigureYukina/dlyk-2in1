#!/usr/bin/env bash
# ============================================================
# 在 Docker 内非 GUI 运行 JMeter 压测
#
# 用法：tools/jmeter/run.sh [并发数] [时长秒数]
#
# 前置：
#   1. 应用已启动（bash tools/app/restart-app.sh false）
#   2. 镜像已构建（见 tools/jmeter/Dockerfile）
#
# 注意：MSYS_NO_PATHCONV=1 是必须的。Git Bash 会把容器内的绝对路径
# /tools/jmeter 自动改写成 Windows 路径，导致 docker 报
# "the working directory ... is invalid"。
# ============================================================
set -euo pipefail

USERS="${1:-50}"
DURATION="${2:-60}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$ROOT/tools/jmeter"

rm -f "$OUT/result.jtl" "$OUT/jmeter.log"
rm -rf "$OUT/report"

MSYS_NO_PATHCONV=1 docker run -d --name jmeter-run \
  -v "$ROOT/tools:/tools" -w /tools/jmeter \
  dlyk/jmeter:5.6.3 \
  -n -t /tools/jmeter/dlyk-crm.jmx \
  -l /tools/jmeter/result.jtl \
  -j /tools/jmeter/jmeter.log \
  -e -o /tools/jmeter/report \
  -Jhost=host.docker.internal -Jport=8089 \
  -Jusers="$USERS" -Jduration="$DURATION"

echo "已启动容器 jmeter-run（$USERS 并发 / ${DURATION}s），等待结束..."

# 容器结束（退出）后退出循环；用 inspect 判断，避免依赖 docker wait 的输出格式
for _ in $(seq 1 120); do
  state="$(docker inspect -f '{{.State.Running}}' jmeter-run 2>/dev/null || echo gone)"
  if [ "$state" = "false" ]; then
    break
  fi
  sleep 5
done

exit_code="$(docker inspect -f '{{.State.ExitCode}}' jmeter-run 2>/dev/null || echo 1)"
docker logs jmeter-run 2>&1 | tail -25
docker rm -f jmeter-run >/dev/null 2>&1 || true

if [ "$exit_code" != "0" ]; then
  echo "JMeter 退出码 $exit_code，详情见 $OUT/jmeter.log" >&2
  exit 1
fi

echo "完成：$OUT/result.jtl，HTML 报告在 $OUT/report/index.html"
