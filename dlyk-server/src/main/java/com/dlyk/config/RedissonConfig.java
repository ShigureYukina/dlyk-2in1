package com.dlyk.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 分布式锁配置
 *
 * <p>直接复用 spring.data.redis.* 的连接参数，避免与既有
 * {@link RedisCacheConfig} 的缓存配置产生两套 Redis 连接来源。
 *
 * <p>Codec 指定为 {@link StringCodec}，使锁 key 在 redis-cli 中保持可读，
 * 便于压测与线上排查时直接观察锁持有情况。
 *
 * @author ShigureYukina
 */
@Configuration
public class RedissonConfig {

    /** 锁 key 统一前缀，避免与业务缓存 key 冲突 */
    public static final String LOCK_KEY_PREFIX = "dlyk:lock:";

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${dlyk.redisson.pool-size:32}")
    private int poolSize;

    @Value("${dlyk.redisson.timeout-ms:3000}")
    private int timeoutMs;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);

        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionPoolSize(poolSize)
                .setConnectionMinimumIdleSize(Math.max(4, poolSize / 8))
                .setTimeout(timeoutMs)
                .setRetryAttempts(3)
                .setRetryInterval(1000);

        if (StringUtils.hasText(password)) {
            server.setPassword(password);
        }

        return Redisson.create(config);
    }
}
