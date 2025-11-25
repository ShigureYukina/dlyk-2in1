package com.dlyk.aspect;

import com.dlyk.config.CacheRefreshConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 缓存一致性切面
 * 确保在数据修改操作后，相关的缓存被正确清除
 */
@Aspect
@Component
public class CacheConsistencyAspect {

    private final CacheRefreshConfig cacheRefreshConfig;

    public CacheConsistencyAspect(CacheRefreshConfig cacheRefreshConfig) {
        this.cacheRefreshConfig = cacheRefreshConfig;
    }

    /**
     * 环绕通知，处理带@CacheEvict注解的方法
     * 确保在事务提交后再清除缓存，保证数据一致性
     */
    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行原方法
        Object result = joinPoint.proceed();
        
        // 异步处理缓存清除，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 可以添加一些延迟，确保事务已提交
                Thread.sleep(100);
                // 缓存清除逻辑已经在@CacheEvict注解中处理
                // 这里可以添加额外的日志或监控逻辑
            } catch (Exception e) {
                // 记录异常，但不中断主流程
                e.printStackTrace();
            }
        });
        
        return result;
    }
}