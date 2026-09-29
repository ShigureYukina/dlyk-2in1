# JMeter 压测镜像
#
# 为什么自己构建：本机只能访问 daocloud 镜像源，而其白名单里没有 JMeter 镜像
# （justb4/jmeter、vinsdocker/jmmeter、rbianco/jmeter 均被拒；bitnami/jmeter 不存在）。
# 基础镜像改用白名单内的 library/eclipse-temurin，
# JMeter 发行包先在宿主机经代理下好，再 COPY 进来 —— 构建过程本身不需要网络。
#
# 构建：
#   curl -x http://127.0.0.1:7890 -sL -o tools/jmeter/apache-jmeter.tgz \
#     https://dlcdn.apache.org/jmeter/binaries/apache-jmeter-5.6.3.tgz
#   docker build -t dlyk/jmeter:5.6.3 tools/jmeter
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre

ARG JMETER_VERSION=5.6.3
ENV JMETER_HOME=/opt/apache-jmeter-${JMETER_VERSION}
ENV PATH=${JMETER_HOME}/bin:${PATH}

COPY apache-jmeter.tgz /tmp/
RUN tar xzf /tmp/apache-jmeter.tgz -C /opt && rm -f /tmp/apache-jmeter.tgz

WORKDIR /load
ENTRYPOINT ["jmeter"]
