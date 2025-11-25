package com.dlyk.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Configuration
@EnableScheduling
public class CacheRefreshConfig {

    private final CacheManager cacheManager;

    public CacheRefreshConfig(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 定时刷新所有缓存，确保数据一致性
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟
    @Caching(evict = {
            @CacheEvict(value = "activityCache", allEntries = true),
            @CacheEvict(value = "ongoingActivityCache", allEntries = true),
            @CacheEvict(value = "userCache", allEntries = true)
    })
    public void refreshAllCaches() {
        // 这个方法的主要目的是通过@CacheEvict注解清除缓存
        // 实际的缓存将在下次访问时重新加载
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