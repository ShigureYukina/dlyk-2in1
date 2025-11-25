package com.dlyk.util;

import com.dlyk.cache.TwoLevelCacheManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 缓存工具类（增强版）
 * 支持双层缓存操作
 * 
 * @author ShigureYukina, Enhanced by Kilo Code
 */
@Component
public class CacheUtils {
    
    private static TwoLevelCacheManager twoLevelCacheManager;
    
    @Resource
    public void setTwoLevelCacheManager(TwoLevelCacheManager manager) {
        CacheUtils.twoLevelCacheManager = manager;
    }
    
    /**
     * 获取缓存数据（原有方法保留兼容性）
     * 
     * @param cacheSelector 缓存查询函数
     * @param databaseSelector 数据库查询函数
     * @param cacheSave 缓存保存函数
     * @param <T> 数据类型
     * @return 查询结果
     */
    public static <T> T getCacheData(Supplier<T> cacheSelector, Supplier<T> databaseSelector, Consumer<T> cacheSave) {
        T data = cacheSelector.get();
        if (Objects.isNull(data)) {
            data = databaseSelector.get();
            if (!Objects.isNull(data)) {
                cacheSave.accept(data);
            }
        }
        return data;
    }
    
    /**
     * 使用双层缓存获取数据（新增方法）
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param loader 数据加载器
     * @param <T> 数据类型
     * @return 查询结果
     */
    public static <T> T getFromTwoLevelCache(String cacheName, String key, Supplier<T> loader) {
        if (twoLevelCacheManager == null) {
            // 降级：直接从数据库加载
            return loader.get();
        }
        return twoLevelCacheManager.get(cacheName, key, loader);
    }
    
    /**
     * 写入双层缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param value 缓存值
     */
    public static void putToTwoLevelCache(String cacheName, String key, Object value) {
        if (twoLevelCacheManager != null && value != null) {
            twoLevelCacheManager.put(cacheName, key, value);
        }
    }
    
    /**
     * 删除双层缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public static void evictFromTwoLevelCache(String cacheName, String key) {
        if (twoLevelCacheManager != null) {
            twoLevelCacheManager.evict(cacheName, key);
        }
    }
    
    /**
     * 清空指定缓存空间
     * 
     * @param cacheName 缓存名称
     */
    public static void clearTwoLevelCache(String cacheName) {
        if (twoLevelCacheManager != null) {
            twoLevelCacheManager.clear(cacheName);
        }
    }
    
    /**
     * 按前缀批量删除缓存
     * 
     * @param cacheName 缓存名称
     * @param keyPrefix key 前缀
     */
    public static void evictByPrefix(String cacheName, String keyPrefix) {
        if (twoLevelCacheManager != null) {
            twoLevelCacheManager.evictByPrefix(cacheName, keyPrefix);
        }
    }
}
