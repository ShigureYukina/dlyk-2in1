package com.dlyk.web;

import com.dlyk.cache.CacheLockManager;
import com.dlyk.cache.CacheWarmupService;
import com.dlyk.cache.TwoLevelCacheManager;
import com.dlyk.result.R;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存管理和监控的 REST API
 * 
 * @author Kilo Code
 */
@RestController
@RequestMapping("/api/cache")
public class CacheManagementController {
    
    private static final Logger log = LoggerFactory.getLogger(CacheManagementController.class);
    
    @Resource
    private TwoLevelCacheManager cacheManager;
    
    @Resource
    private CacheWarmupService cacheWarmupService;
    
    @Resource
    private CacheLockManager cacheLockManager;
    
    /**
     * 清除指定缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @return 操作结果
     */
    @DeleteMapping("/evict")
    public R evictCache(@RequestParam String cacheName, @RequestParam String key) {
        try {
            cacheManager.evict(cacheName, key);
            log.info("缓存已清除，cacheName: {}, key: {}", cacheName, key);
            return R.OK("缓存清除成功");
        } catch (Exception e) {
            log.error("缓存清除失败", e);
            return R.FAIL("缓存清除失败: " + e.getMessage());
        }
    }
    
    /**
     * 按前缀批量清除缓存
     * 
     * @param cacheName 缓存名称
     * @param keyPrefix key 前缀
     * @return 操作结果
     */
    @DeleteMapping("/evictByPrefix")
    public R evictCacheByPrefix(@RequestParam String cacheName, @RequestParam String keyPrefix) {
        try {
            cacheManager.evictByPrefix(cacheName, keyPrefix);
            log.info("缓存已按前缀清除，cacheName: {}, prefix: {}", cacheName, keyPrefix);
            return R.OK("缓存批量清除成功");
        } catch (Exception e) {
            log.error("缓存批量清除失败", e);
            return R.FAIL("缓存批量清除失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空指定缓存空间
     * 
     * @param cacheName 缓存名称
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    public R clearCache(@RequestParam String cacheName) {
        try {
            cacheManager.clear(cacheName);
            log.info("缓存空间已清空，cacheName: {}", cacheName);
            return R.OK("缓存空间清空成功");
        } catch (Exception e) {
            log.error("缓存空间清空失败", e);
            return R.FAIL("缓存空间清空失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发缓存预热
     * 
     * @return 操作结果
     */
    @PostMapping("/warmup")
    public R manualWarmup() {
        try {
            cacheWarmupService.manualWarmup();
            log.info("手动缓存预热已触发");
            return R.OK("缓存预热成功");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
            return R.FAIL("缓存预热失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public R getCacheStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 锁管理器统计
            stats.put("lockMapSize", cacheLockManager.getLockMapSize());
            
            // 可以添加更多统计信息
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("message", "缓存统计信息");
            
            return R.OK(stats);
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return R.FAIL("获取缓存统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理未使用的锁
     * 
     * @return 操作结果
     */
    @PostMapping("/clearUnusedLocks")
    public R clearUnusedLocks() {
        try {
            cacheLockManager.clearUnusedLocks();
            log.info("未使用的锁已清理");
            return R.OK("锁清理成功");
        } catch (Exception e) {
            log.error("锁清理失败", e);
            return R.FAIL("锁清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public R healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("cacheManager", "RUNNING");
        health.put("lockManager", "RUNNING");
        health.put("timestamp", System.currentTimeMillis());
        return R.OK(health);
    }
}