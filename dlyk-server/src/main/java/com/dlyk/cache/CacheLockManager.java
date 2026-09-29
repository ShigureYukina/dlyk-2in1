package com.dlyk.cache;

import com.dlyk.config.RedissonConfig;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁管理器（基于 Redisson）
 *
 * <p>原实现使用 {@code ConcurrentHashMap<String, ReentrantLock>}，属于进程内锁：
 * 单机并发下能挡住缓存击穿，但应用一旦多实例部署，各实例持有的是各自的锁，
 * 互斥语义失效。此处改为 Redisson 的 {@link RLock}，由 Redis 统一仲裁，
 * 多实例部署下仍保证同一 key 同一时刻只有一个执行者。
 *
 * <p>锁通过租约（leaseTime）自动过期：持锁进程崩溃时，锁最多在 leaseTime 后自动释放，
 * 不会像进程内锁那样产生无法回收的死锁。释放前会校验持有者线程，防止超时后误删他人锁。
 *
 * @author ShigureYukina
 */
@Component
public class CacheLockManager {

    private static final Logger log = LoggerFactory.getLogger(CacheLockManager.class);

    /** 默认最长等待获取锁的时间（秒） */
    private static final long DEFAULT_WAIT_TIME = 3;

    /** 默认锁租约时间（秒），到期自动释放，防止持锁进程崩溃导致死锁 */
    private static final long DEFAULT_LEASE_TIME = 5;

    private static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 本实例当前持有的锁 key，仅用于本地可观测性统计。
     * 真正的互斥由 Redis 保证，该集合不参与任何加锁判断。
     */
    private final Set<String> heldLockKeys = ConcurrentHashMap.newKeySet();

    /**
     * 获取指定 key 对应的分布式锁对象。
     *
     * @param key 锁 key（业务语义，不含统一前缀）
     * @return Redisson 分布式锁
     */
    public RLock getLock(String key) {
        return redissonClient.getLock(RedissonConfig.LOCK_KEY_PREFIX + key);
    }

    /**
     * 尝试获取锁，使用默认等待时间与租约时间。
     *
     * @param key 锁 key
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, DEFAULT_UNIT);
    }

    /**
     * 尝试获取锁，自定义等待时间，租约时间取默认值。
     *
     * @param key       锁 key
     * @param waitTime  最长等待时间
     * @param unit      时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key, long waitTime, TimeUnit unit) {
        return tryLock(key, waitTime, DEFAULT_LEASE_TIME, unit);
    }

    /**
     * 尝试获取锁，自定义等待时间与租约时间。
     *
     * @param key       锁 key
     * @param waitTime  最长等待获取锁的时间
     * @param leaseTime 锁租约时间，到期自动释放
     * @param unit      时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(key);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                heldLockKeys.add(key);
            } else {
                log.warn("获取分布式锁失败，key: {}, 等待时间: {} {}", key, waitTime, unit);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，key: {}", key, e);
            return false;
        }
    }

    /**
     * 释放锁。仅当锁由当前线程持有时才释放，避免租约超时后误删其他实例的锁。
     *
     * @param key 锁 key
     */
    public void unlock(String key) {
        RLock lock = getLock(key);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            } else {
                log.warn("锁已不属于当前线程，跳过释放（可能已因租约到期自动释放），key: {}", key);
            }
        } catch (IllegalMonitorStateException e) {
            // 租约到期后锁已被 Redis 回收，属于正常情况，无需中断业务流程
            log.warn("锁已被自动释放，key: {}", key);
        } finally {
            heldLockKeys.remove(key);
        }
    }

    /**
     * 在分布式锁保护下执行操作，执行完成后自动释放锁。
     *
     * @param key    锁 key
     * @param action 需要执行的操作
     * @param <T>    返回值类型
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
            throw new RuntimeException("无法获取分布式锁: " + key);
        }
    }

    /**
     * 锁操作的函数式接口。
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }

    /**
     * 当前实例持有的锁数量（可观测性指标，非全局锁数量）。
     *
     * @return 本实例持锁数
     */
    public int getLockMapSize() {
        return heldLockKeys.size();
    }

    /**
     * 清理本实例的持锁统计。
     *
     * <p>Redisson 的锁由租约自动回收，无需像进程内锁那样清理锁对象；
     * 此方法仅重置本地统计，用于监控数据修正。
     */
    public void clearUnusedLocks() {
        heldLockKeys.clear();
        log.info("已重置本实例持锁统计，当前持锁数: 0");
    }
}
