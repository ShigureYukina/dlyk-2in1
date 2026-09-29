package com.dlyk.task;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 缓存定时刷新任务（原 {@code CacheRefreshConfig} 中的 @Scheduled 已迁移至此）
 *
 * <p>显式调用 {@link CacheManager#getCache} 清理，而不是依赖 {@code @CacheEvict} 注解：
 * 定时任务由 XXL-Job 反射调度，若依赖注解则需要保证代理链路完整，
 * 显式清理在调度场景下行为更可预期、也更容易在对账日志里核对。
 *
 * @author ShigureYukina
 */
@Component
public class CacheRefreshJobHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheRefreshJobHandler.class);

    /** 需要周期性失效的缓存空间 */
    private static final String[] PERIODIC_REFRESH_CACHES = {
            "activityCache", "ongoingActivityCache", "userCache"
    };

    @Resource
    private CacheManager cacheManager;

    @XxlJob("cacheRefreshJobHandler")
    public void refreshCache() {
        int cleared = 0;
        for (String cacheName : PERIODIC_REFRESH_CACHES) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                cleared++;
            }
        }
        String message = "定时缓存刷新完成，已清理缓存空间 " + cleared + " 个";
        log.info(message);
        XxlJobHelper.log(message);
    }
}
