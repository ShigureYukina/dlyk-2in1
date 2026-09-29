package com.dlyk.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * 缓存刷新辅助配置
 *
 * <p>原 {@code @Scheduled} 定时刷新已迁移到 XXL-Job 的
 * {@code cacheRefreshJobHandler}，本类只保留事务后清缓存等辅助能力。
 */
@Configuration
public class CacheRefreshConfig {

    private final CacheManager cacheManager;

    public CacheRefreshConfig(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 手动清除所有缓存的方法
     */
    public void clearAllCaches() {
        // 获取所有缓存名称
        Collection<String> cacheNames = cacheManager.getCacheNames();
        
        // 清除每个缓存中的所有条目
        for (String cacheName : cacheNames) {
            cacheManager.getCache(cacheName).clear();
        }
    }

    /**
     * 在事务提交后清除指定缓存
     * 确保数据一致性
     */
    @Transactional
    public void clearCacheAfterTransaction(String... cacheNames) {
        for (String cacheName : cacheNames) {
            cacheManager.getCache(cacheName).clear();
        }
    }
}