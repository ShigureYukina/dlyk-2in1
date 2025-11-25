package com.dlyk.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存锁管理器
 * 用于防止缓存击穿，为每个缓存 key 维护独立的互斥锁
 * 
 * @author Kilo Code
 */
@Component
public class CacheLockManager {
    
    private static final Logger log = LoggerFactory.getLogger(CacheLockManager.class);
    
    /**
     * 存储每个 key 对应的锁
     */
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    
    /**
     * 锁的默认超时时间（秒）
     */
    private static final long DEFAULT_LOCK_TIMEOUT = 5;
    
    /**
     * 获取指定 key 的锁
     * 
     * @param key 缓存键
     * @return 对应的锁对象
     */
    public ReentrantLock getLock(String key) {
        return lockMap.computeIfAbsent(key, k -> new ReentrantLock());
    }
    
    /**
     * 尝试获取锁，带超时机制
     * 
     * @param key 缓存键
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_LOCK_TIMEOUT, TimeUnit.SECONDS);
    }
    
    /**
     * 尝试获取锁，带自定义超时时间
     * 
     * @param key 缓存键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        ReentrantLock lock = getLock(key);
        try {
            boolean acquired = lock.tryLock(timeout, unit);
            if (!acquired) {
                log.warn("获取缓存锁失败，key: {}, 超时时间: {} {}", key, timeout, unit);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取缓存锁被中断，key: {}", key, e);
            return false;
        }
    }
    
    /**
     * 释放锁
     * 
     * @param key 缓存键
     */
    public void unlock(String key) {
        ReentrantLock lock = lockMap.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            // 如果锁没有被任何线程持有，则从 map 中移除以释放内存
            if (!lock.isLocked()) {
                lockMap.remove(key);
            }
        }
    }
    
    /**
     * 执行带锁的操作
     * 
     * @param key 缓存键
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String key, LockAction<T> action) {
        if (tryLock(key)) {
            try {
                return action.execute();
            } finally {
                unlock(key);
            }
        } else {
            throw new RuntimeException("无法获取缓存锁: " + key);
        }
    }
    
    /**
     * 锁操作的函数式接口
     * 
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }
    
    /**
     * 获取当前锁映射的大小（用于监控）
     * 
     * @return 锁的数量
     */
    public int getLockMapSize() {
        return lockMap.size();
    }
    
    /**
     * 清理所有未使用的锁
     */
    public void clearUnusedLocks() {
        lockMap.entrySet().removeIf(entry -> !entry.getValue().isLocked());
        log.info("清理未使用的锁，当前锁数量: {}", lockMap.size());
    }
}