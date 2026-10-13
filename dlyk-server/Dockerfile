# dlyk-server 应用镜像（构建上下文 = dlyk-server/ 目录）
#
# 多阶段构建：Maven 构建层 → layertools 分层提取 → JRE 运行层。
# 分层让"依赖基本不变、代码常变"的镜像增量只有 application 层，
# 配合 docker-compose.yml 的 app 服务组成完整的应用镜像 → 编排链路。
#
# IMAGE_PREFIX：基础镜像仓库前缀（直连 Docker Hub 不通的环境设为
#   docker.m.daocloud.io/library/，与 compose 的 .env 约定一致；
#   CI 的 GitHub Actions 直连 Docker Hub，用默认空值即可）。

ARG IMAGE_PREFIX=

FROM ${IMAGE_PREFIX}maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# 先拷 pom 拉依赖（依赖不变时这层缓存稳定），再拷源码打包
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# 提取 Spring Boot 分层 jar（Boot 2.4+ 默认分层）
FROM ${IMAGE_PREFIX}eclipse-temurin:17-jre-jammy AS extract
WORKDIR /app
COPY --from=build /build/target/dlyk-server-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM ${IMAGE_PREFIX}eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --from=extract --chown=appuser /app/dependencies/ ./
COPY --from=extract --chown=appuser /app/spring-boot-loader/ ./
COPY --from=extract --chown=appuser /app/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser /app/application/ ./
USER appuser
# 8089 业务端口 / 9090 管理端口（actuator 只在 9090 暴露）
EXPOSE 8089 9090
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]
