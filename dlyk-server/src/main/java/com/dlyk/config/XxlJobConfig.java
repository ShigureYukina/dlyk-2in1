package com.dlyk.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器配置
 *
 * <p>把原先散落在各处的 {@code @Scheduled} 定时任务统一收敛到调度中心管理：
 * 调度策略、失败重试、分片广播、执行日志与告警都由调度平台承担，
 * 应用侧只保留任务逻辑，不再各自维护 cron 表达式。
 *
 * <p>当 {@code xxl.job.enabled=false} 时执行器不注册，应用仍可正常启动
 * （例如本地只调接口、不连调度中心的场景）。
 *
 * @author ShigureYukina
 */
@Configuration
@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobConfig {

    private static final Logger log = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${xxl.job.admin.addresses:}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname:dlyk-server-executor}")
    private String appName;

    @Value("${xxl.job.executor.address:}")
    private String address;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port:9999}")
    private int port;

    @Value("${xxl.job.executor.logpath:./logs/xxl-job}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:7}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("初始化 XXL-Job 执行器，appname: {}，调度中心: {}，端口: {}", appName, adminAddresses, port);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appName);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
