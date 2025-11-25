package com.dlyk.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * 本地缓存管理器
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 设置本地缓存过期时间为5分钟
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(1000));
        return cacheManager;
    }

    /**
     * Redis缓存管理器
     */
    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 设置Redis缓存过期时间为30分钟
                .entryTtl(Duration.ofMinutes(30))
                // 设置key的序列化方式
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 设置value的序列化方式
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 不缓存null值
                .disableCachingNullValues();

        // 特定缓存配置
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        // 活动缓存设置较短的过期时间
        configs.put("activityCache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        // 用户缓存设置较长的过期时间
        configs.put("userCache", defaultConfig.entryTtl(Duration.ofHours(1)));
        // 进行中的活动缓存
        configs.put("ongoingActivityCache", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }

    /**
     * 自定义缓存key生成器
     */
    @Bean
    public KeyGenerator cacheKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append(":");
            sb.append(method.getName());
            sb.append(":");
            for (Object param : params) {
                sb.append(param.toString()).append(",");
            }
            return sb.toString();
        };
    }
    
    /**
     * 缓存刷新配置Bean
     */
    @Bean
    public CacheRefreshConfig cacheRefreshConfig(CacheManager cacheManager) {
        return new CacheRefreshConfig(cacheManager);
    }
}