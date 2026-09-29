#!/usr/bin/env bash
# ============================================================
# 重启后端应用（宿主 8089；nginx 容器通过 host.docker.internal:8089 访问）
#
# 用法：tools/app/restart-app.sh [XXL_JOB_ENABLED] [--build]
#
#   XXL_JOB_ENABLED 默认 false。
#   压测时必须传 false：缓存预热与对账定时任务本身会打库，
#   混进来会污染 Com_select 统计，让"有缓存/无缓存"两轮不可比。
#   日常运行传 true。
#
#   --build 先停进程再执行 mvn package。
#   顺序很关键：Windows 上正在运行的 java 会锁住 jar，
#   先打包会以 "Unable to rename ...jar to ...jar.original" 失败，
#   并且失败后 target 下会留下一个没有 Main-Class 的薄 jar。
#
# 其它可透传的环境变量（在调用前 export 即可，java 进程会继承）：
#   LIST_TOTAL_CACHE_ENABLED=false   旁路列表 total 缓存，用于压测对照组
#   DLYK_MQ_ENABLED=false            不起 RabbitMQ 时不发布/消费事件
#
# 依赖：JDK / Maven 不在 PATH，脚本内显式 export。
# ============================================================
set -euo pipefail

export JAVA_HOME=/d/test/tools/jdk-17.0.20.1+1
export PATH="$JAVA_HOME/bin:/d/test/tools/apache-maven-3.9.16/bin:$PATH"

APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/dlyk-server"
JAR="$APP_HOME/target/dlyk-server-0.0.1-SNAPSHOT.jar"
LOG="$APP_HOME/logs/app-console.log"
XXL_JOB_ENABLED="${1:-false}"
BUILD="${2:-}"

# 按端口定位旧进程，不依赖外部记录的 PID
OLD_PIDS="$(netstat -ano | grep LISTENING | grep -E ':8089[[:space:]]' | awk '{print $5}' | sort -u || true)"
for pid in $OLD_PIDS; do
  echo "停止旧进程 pid=$pid"
  taskkill //PID "$pid" //F >/dev/null 2>&1 || true
done
[ -n "$OLD_PIDS" ] && sleep 2

if [ "$BUILD" = "--build" ]; then
  echo "打包（mvn -o -DskipTests -Dmaven.test.skip=true package）"
  ( cd "$APP_HOME" && mvn -q -o -Dmaven.test.skip=true package )
fi

if [ ! -f "$JAR" ]; then
  echo "未找到 jar：$JAR" >&2
  exit 1
fi
# 薄 jar 没有 BOOT-INF，起不来（会报"没有主清单属性"），提前拦住
if ! unzip -l "$JAR" | grep -q "BOOT-INF/"; then
  echo "jar 不是可执行包（缺 BOOT-INF）：$JAR，请重新打包" >&2
  exit 1
fi

echo "启动应用：XXL_JOB_ENABLED=$XXL_JOB_ENABLED"
XXL_JOB_ENABLED="$XXL_JOB_ENABLED" nohup "$JAVA_HOME/bin/java" -jar "$JAR" >"$LOG" 2>&1 &

# 轮询健康检查：应用要初始化 Redis/Redisson/MQ/数据源，通常 20~40s
for i in $(seq 1 60); do
  code="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8089/actuator/health || true)"
  if [ "$code" = "200" ]; then
    echo "应用已就绪（第 ${i} 次轮询），健康检查 200"
    exit 0
  fi
  sleep 2
done

echo "启动超时，最后 40 行日志：" >&2
tail -40 "$LOG" >&2
exit 1
