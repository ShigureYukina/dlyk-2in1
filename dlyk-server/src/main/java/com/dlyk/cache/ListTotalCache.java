package com.dlyk.cache;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * 列表 total 缓存。
 *
 * <p>列表接口的 total 是纯统计值：它不参与筛选（见 ClueServiceImpl#getCluePage 的说明），
 * 只随表内新增/逻辑删除变化，所以可以接受秒级滞后。而每翻一页都要打一次 COUNT，
 * 在压测里这部分是纯粹的重复查询，用一个固定 key + 10 秒 TTL 挡掉即可。
 *
 * <p>key 只按表名划分，不拼筛选条件：total 本身与筛选无关，因此 key 数量恒等于被统计的表数，
 * 不存在过滤条件组合把 key 数量放大的问题。
 *
 * <p>不做"写操作主动失效"：删除缓存后并发读仍可能把旧值写回，留下的滞后窗口和 TTL 一样大，
 * 却引入跨模块调用与新的竞态。10 秒 TTL 更简单，缓存/Redis 异常时也能自愈。
 */
@Component
public class ListTotalCache {

    private static final Logger log = LoggerFactory.getLogger(ListTotalCache.class);

    /**
     * 缓存名，用于让 {@code DELETE /api/cache/clear?cacheName=listTotalCache} 能清掉它。
     * 数据不在 CacheManager 里（是裸 Redis key），所以控制器只认这个名字做特判。
     */
    public static final String CACHE_NAME = "listTotalCache";

    private static final String KEY_PREFIX = "dlyk:list:total:";

    private static final Duration TTL = Duration.ofSeconds(10);

    private static final String METRIC_GETS = "dlyk.list.total.cache.gets";

    /**
     * 旁路开关。压测时用它构造"没有这层缓存"的对照组，
     * 比在外部定时删 key 更精确（删除与写入存在竞态，命中率压不到 0）。
     */
    @Value("${dlyk.list-total-cache.enabled:true}")
    private boolean enabled;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 读取列表 total，未命中时调用 loader 查库并回填。
     *
     * <p>缓存读写异常只降级为直接查库，不影响业务。
     *
     * @param table  被统计的表名，作为缓存 key
     * @param loader 查库逻辑
     * @return 列表总数
     */
    public long get(String table, LongSupplier loader) {
        if (!enabled) {
            return loader.getAsLong();
        }

        String key = KEY_PREFIX + table;

        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                count(METRIC_GETS, "hit");
                return Long.parseLong(cached);
            }
        } catch (Exception e) {
            log.warn("读取列表 total 缓存失败，降级为直接查库。key: {}", key, e);
        }
        count(METRIC_GETS, "miss");

        long total = loader.getAsLong();
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(total), TTL);
        } catch (Exception e) {
            log.warn("回填列表 total 缓存失败。key: {}", key, e);
        }
        return total;
    }

    /**
     * 清空全部 total 缓存。
     *
     * <p>给压测的"无缓存"对照组用：不清掉它，基线里仍然带着这层缓存，
     * 会把优化效果算小。
     *
     * <p>这里用了 KEYS，只因为命名空间很小（每个被统计的表一个 key）且仅由测试调用；
     * 生产环境应避免 KEYS，改用 SCAN 或维护一份 key 清单。
     *
     * @return 删除的 key 数量
     */
    public long clear() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        return stringRedisTemplate.delete(keys);
    }

    /**
     * 记录缓存命中/未命中。
     *
     * <p>自己埋点而不是依赖 actuator 的 cache_gets_total：项目用的是自定义 CacheManager Bean，
     * 该指标不会自增（而 Redis 侧的 keyspace 统计又不区分业务缓存），只有自埋点才能得到真实命中率。
     */
    private void count(String name, String hitOrMiss) {
        meterRegistry.counter(name, "result", hitOrMiss).increment();
    }
}
