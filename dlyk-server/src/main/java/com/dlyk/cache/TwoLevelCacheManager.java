package com.dlyk.cache;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 双层缓存管理器
 * 统一管理 Caffeine 本地缓存（L1）和 Redis 分布式缓存（L2）
 * 实现了缓存穿透、缓存击穿、缓存雪崩的防护机制
 * 
 * @author Kilo Code
 */
@Component
public class TwoLevelCacheManager {
    
    private static final Logger log = LoggerFactory.getLogger(TwoLevelCacheManager.class);
    
    /**
     * Caffeine 本地缓存管理器
     */
    @Resource(name = "caffeineCacheManager")
    private CacheManager caffeineCacheManager;
    
    /**
     * Redis 缓存管理器
     */
    @Resource(name = "redisCacheManager")
    private CacheManager redisCacheManager;
    
    /**
     * Redis 操作模板
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 缓存锁管理器
     */
    @Resource
    private CacheLockManager cacheLockManager;
    
    /**
     * 空值缓存的默认过期时间（分钟）
     */
    private static final long NULL_VALUE_TTL = 2;
    
    /**
     * 随机过期时间的最大偏移量（分钟）
     */
    private static final long RANDOM_TTL_OFFSET = 5;
    
    /**
     * 随机数生成器
     */
    private static final Random RANDOM = new Random();
    
    /**
     * 从双层缓存获取数据
     * 查询顺序：L1（本地缓存） -> L2（Redis缓存） -> 数据库
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param loader 数据加载器（从数据库加载）
     * @param <T> 数据类型
     * @return 查询结果
     */
    public <T> T get(String cacheName, String key, Supplier<T> loader) {
        // 1. 查询本地缓存
        T value = getFromL1Cache(cacheName, key);
        if (value != null) {
            log.debug("本地缓存命中，cacheName: {}, key: {}", cacheName, key);
            return handleNullValue(value);
        }
        
        // 2. 查询 Redis 缓存
        value = getFromL2Cache(cacheName, key);
        if (value != null) {
            log.debug("Redis缓存命中，cacheName: {}, key: {}", cacheName, key);
            // 回填本地缓存
            putToL1Cache(cacheName, key, value);
            return handleNullValue(value);
        }
        
        // 3. 缓存未命中，从数据库加载（带互斥锁防击穿）
        return getWithLock(cacheName, key, loader);
    }
    
    /**
     * 带互斥锁的数据加载（防止缓存击穿）
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param loader 数据加载器
     * @param <T> 数据类型
     * @return 查询结果
     */
    private <T> T getWithLock(String cacheName, String key, Supplier<T> loader) {
        String lockKey = cacheName + ":" + key;
        
        // 尝试获取锁
        if (cacheLockManager.tryLock(lockKey)) {
            try {
                // 双重检查：再次查询缓存，防止重复加载
                T value = getFromL2Cache(cacheName, key);
                if (value != null) {
                    putToL1Cache(cacheName, key, value);
                    return handleNullValue(value);
                }
                
                // 从数据库加载数据
                log.debug("缓存未命中，从数据库加载，cacheName: {}, key: {}", cacheName, key);
                value = loader.get();
                
                if (value != null) {
                    // 存在数据，写入双层缓存（带随机过期时间防雪崩）
                    put(cacheName, key, value);
                } else {
                    // 数据不存在，缓存空值防穿透
                    putNullValue(cacheName, key);
                }
                
                return value;
            } finally {
                cacheLockManager.unlock(lockKey);
            }
        } else {
            // 获取锁失败，等待后重试
            log.debug("获取锁失败，等待重试，cacheName: {}, key: {}", cacheName, key);
            try {
                Thread.sleep(50); // 短暂等待
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 重新查询缓存（此时可能已被其他线程加载）
            return get(cacheName, key, loader);
        }
    }
    
    /**
     * 写入双层缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param value 缓存值
     */
    public void put(String cacheName, String key, Object value) {
        if (value == null) {
            return;
        }
        
        // 写入本地缓存
        putToL1Cache(cacheName, key, value);
        
        // 写入 Redis 缓存（带随机过期时间）
        putToL2CacheWithRandomTTL(cacheName, key, value);
        
        log.debug("数据已写入双层缓存，cacheName: {}, key: {}", cacheName, key);
    }
    
    /**
     * 删除双层缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void evict(String cacheName, String key) {
        // 删除本地缓存
        evictFromL1Cache(cacheName, key);
        
        // 删除 Redis 缓存
        evictFromL2Cache(cacheName, key);
        
        log.debug("缓存已删除，cacheName: {}, key: {}", cacheName, key);
    }
    
    /**
     * 清空指定缓存空间的所有数据
     * 
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        // 清空本地缓存
        Cache l1Cache = caffeineCacheManager.getCache(cacheName);
        if (l1Cache != null) {
            l1Cache.clear();
        }
        
        // 清空 Redis 缓存
        Cache l2Cache = redisCacheManager.getCache(cacheName);
        if (l2Cache != null) {
            l2Cache.clear();
        }
        
        log.info("缓存空间已清空，cacheName: {}", cacheName);
    }
    
    /**
     * 从本地缓存获取数据
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromL1Cache(String cacheName, String key) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
        }
        return null;
    }
    
    /**
     * 从 Redis 缓存获取数据
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromL2Cache(String cacheName, String key) {
        Cache cache = redisCacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
        }
        return null;
    }
    
    /**
     * 写入本地缓存
     */
    private void putToL1Cache(String cacheName, String key, Object value) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }
    
    /**
     * 写入 Redis 缓存（带随机过期时间防雪崩）
     */
    private void putToL2CacheWithRandomTTL(String cacheName, String key, Object value) {
        Cache cache = redisCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
        
        // 为 Redis key 添加随机过期时间偏移
        String redisKey = buildRedisKey(cacheName, key);
        long randomOffset = RANDOM.nextInt((int) RANDOM_TTL_OFFSET * 60); // 转换为秒
        redisTemplate.expire(redisKey, 30 * 60 + randomOffset, TimeUnit.SECONDS);
    }
    
    /**
     * 缓存空值（防穿透）
     */
    private void putNullValue(String cacheName, String key) {
        // 本地缓存空值
        putToL1Cache(cacheName, key, NullValue.INSTANCE);
        
        // Redis 缓存空值（较短的过期时间）
        Cache cache = redisCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, NullValue.INSTANCE);
        }
        
        // 设置较短的过期时间
        String redisKey = buildRedisKey(cacheName, key);
        redisTemplate.expire(redisKey, NULL_VALUE_TTL, TimeUnit.MINUTES);
        
        log.debug("已缓存空值，cacheName: {}, key: {}", cacheName, key);
    }
    
    /**
     * 删除本地缓存
     */
    private void evictFromL1Cache(String cacheName, String key) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
    
    /**
     * 删除 Redis 缓存
     */
    private void evictFromL2Cache(String cacheName, String key) {
        Cache cache = redisCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
    
    /**
     * 处理空值对象
     */
    @SuppressWarnings("unchecked")
    private <T> T handleNullValue(T value) {
        if (value instanceof NullValue) {
            return null;
        }
        return value;
    }
    
    /**
     * 构建 Redis 的实际 key
     */
    private String buildRedisKey(String cacheName, String key) {
        return cacheName + "::" + key;
    }
    
    /**
     * 批量删除缓存（按前缀）
     * 
     * @param cacheName 缓存名称
     * @param keyPrefix key 前缀
     */
    public void evictByPrefix(String cacheName, String keyPrefix) {
        // 删除 Redis 缓存（支持模糊匹配）
        String pattern = buildRedisKey(cacheName, keyPrefix + "*");
        redisTemplate.keys(pattern).forEach(key -> 
            redisTemplate.delete(key)
        );
        
        // 本地缓存无法按前缀删除，清空整个缓存空间
        Cache l1Cache = caffeineCacheManager.getCache(cacheName);
        if (l1Cache != null) {
            l1Cache.clear();
        }
        
        log.info("已按前缀删除缓存，cacheName: {}, prefix: {}", cacheName, keyPrefix);
    }
}