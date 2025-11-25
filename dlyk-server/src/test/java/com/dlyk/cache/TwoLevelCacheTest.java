package com.dlyk.cache;

import com.dlyk.DlykServerApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双层缓存功能测试类
 * 用于验证L1（Caffeine）和L2（Redis）缓存的协同工作
 */
@SpringBootTest(classes = DlykServerApplication.class)
class TwoLevelCacheTest {

    @Autowired
    private TwoLevelCacheManager twoLevelCacheManager;

    @Autowired
    private CacheManager caffeineCacheManager;

    @Autowired
    private CacheManager redisCacheManager;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        if (caffeineCacheManager.getCache("testCache") != null) {
            caffeineCacheManager.getCache("testCache").clear();
        }
        if (redisCacheManager.getCache("testCache") != null) {
            redisCacheManager.getCache("testCache").clear();
        }
    }

    @Test
    void testCacheManagerInitialization() {
        System.out.println("=== testCacheManagerInitialization ===");
        System.out.println("目标返回: 所有缓存管理器都应正确注入");

        // 验证缓存管理器是否正确初始化
        assertNotNull(twoLevelCacheManager, "TwoLevelCacheManager应该被正确注入");
        assertNotNull(caffeineCacheManager, "CaffeineCacheManager应该被正确注入");
        assertNotNull(redisCacheManager, "RedisCacheManager应该被正确注入");
        assertNotNull(applicationContext, "ApplicationContext应该被正确注入");

        System.out.println("实际返回: 所有缓存管理器均已正确注入");
        System.out.println("测试结果: 通过");
    }

    @Test
    void testPutAndGetFromCache() {
        System.out.println("=== testPutAndGetFromCache ===");

        // 测试数据
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";

        System.out.println("目标返回: 成功存入缓存并正确获取，期望值=" + value);

        // 写入双层缓存
        twoLevelCacheManager.put(cacheName, key, value);

        // 从双层缓存获取数据
        String result = twoLevelCacheManager.get(cacheName, key, () -> null);

        System.out.println("实际返回: 获取到的值=" + result);

        // 验证结果
        assertEquals(value, result, "从缓存中获取的值应该与写入的值一致");
        System.out.println("测试结果: 通过");
    }

    @Test
    void testL1CacheHit() {
        System.out.println("=== testL1CacheHit ===");

        // 测试数据
        String cacheName = "testCache";
        String key = "l1Key";
        String value = "l1Value";
        String defaultValue = "defaultValue";

        System.out.println("目标返回: 从L1缓存命中数据，期望值=" + value);

        // 直接写入L1缓存
        if (caffeineCacheManager.getCache(cacheName) != null) {
            caffeineCacheManager.getCache(cacheName).put(key, value);
        }

        // 通过双层缓存管理器获取数据
        String result = twoLevelCacheManager.get(cacheName, key, () -> defaultValue);

        System.out.println("实际返回: 获取到的值=" + result);

        // 验证结果 - 应该从L1缓存获取，而不是默认值
        assertEquals(value, result, "应该从L1缓存命中数据");
        System.out.println("测试结果: 通过");
    }

    @Test
    void testL2CacheHit() {
        System.out.println("=== testL2CacheHit ===");

        // 测试数据
        String cacheName = "testCache";
        String key = "l2Key";
        String value = "l2Value";
        String defaultValue = "defaultValue";

        System.out.println("目标返回: 从L2缓存命中数据并回填L1，期望值=" + value);

        // 清理L1缓存，确保不会从L1缓存命中
        if (caffeineCacheManager.getCache(cacheName) != null) {
            caffeineCacheManager.getCache(cacheName).evict(key);
        }

        // 直接写入L2缓存
        if (redisCacheManager.getCache(cacheName) != null) {
            redisCacheManager.getCache(cacheName).put(key, value);
        }

        // 通过双层缓存管理器获取数据
        String result = twoLevelCacheManager.get(cacheName, key, () -> defaultValue);

        System.out.println("实际返回: 获取到的值=" + result);

        // 验证数据是否已回填到L1缓存
        Object l1Value = null;
        if (caffeineCacheManager.getCache(cacheName) != null) {
            l1Value = caffeineCacheManager.getCache(cacheName).get(key, String.class);
        }
        System.out.println("L1缓存回填结果: " + l1Value);

        // 验证结果 - 应该从L2缓存获取，而不是默认值
        assertEquals(value, result, "应该从L2缓存命中数据");
        assertEquals(value, l1Value, "数据应该已回填到L1缓存");
        System.out.println("测试结果: 通过");
    }

    @Test
    void testDatabaseLoad() {
        System.out.println("=== testDatabaseLoad ===");

        // 测试数据
        String cacheName = "testCache";
        String key = "dbKey";
        String dbValue = "dbValue";

        System.out.println("目标返回: 从数据库加载数据并缓存，期望值=" + dbValue);

        // 清理L1和L2缓存
        if (caffeineCacheManager.getCache(cacheName) != null) {
            caffeineCacheManager.getCache(cacheName).evict(key);
        }
        if (redisCacheManager.getCache(cacheName) != null) {
            redisCacheManager.getCache(cacheName).evict(key);
        }

        // 通过双层缓存管理器获取数据，此时应该从"数据库"加载
        String result = twoLevelCacheManager.get(cacheName, key, () -> dbValue);

        System.out.println("实际返回: 从数据库加载的值=" + result);

        // 验证数据是否已写入L1和L2缓存
        Object l1Value = null;
        Object l2Value = null;
        if (caffeineCacheManager.getCache(cacheName) != null) {
            l1Value = caffeineCacheManager.getCache(cacheName).get(key, String.class);
        }
        if (redisCacheManager.getCache(cacheName) != null) {
            l2Value = redisCacheManager.getCache(cacheName).get(key, String.class);
        }

        System.out.println("L1缓存存储结果: " + l1Value);
        System.out.println("L2缓存存储结果: " + l2Value);

        assertEquals(dbValue, result, "应该从数据库加载数据");
        assertEquals(dbValue, l1Value, "数据应该已写入L1缓存");
        assertEquals(dbValue, l2Value, "数据应该已写入L2缓存");
        System.out.println("测试结果: 通过");
    }

    @Test
    void testCacheEvict() {
        System.out.println("=== testCacheEvict ===");

        // 测试数据
        String cacheName = "testCache";
        String key = "evictKey";
        String value = "evictValue";

        System.out.println("目标返回: 缓存数据被成功删除");

        // 写入数据到双层缓存
        twoLevelCacheManager.put(cacheName, key, value);

        // 验证数据存在于缓存中
        String result = twoLevelCacheManager.get(cacheName, key, () -> null);
        System.out.println("删除前获取到的值: " + result);
        assertEquals(value, result, "数据应该存在于缓存中");

        // 删除双层缓存
        twoLevelCacheManager.evict(cacheName, key);

        System.out.println("已执行缓存删除操作");

        // 验证数据已从缓存中删除
        Object l1Value = null;
        Object l2Value = null;
        if (caffeineCacheManager.getCache(cacheName) != null) {
            var wrapper = caffeineCacheManager.getCache(cacheName).get(key);
            l1Value = wrapper != null ? wrapper.get() : null;
        }
        if (redisCacheManager.getCache(cacheName) != null) {
            var wrapper = redisCacheManager.getCache(cacheName).get(key);
            l2Value = wrapper != null ? wrapper.get() : null;
        }

        System.out.println("L1缓存删除后结果: " + l1Value);
        System.out.println("L2缓存删除后结果: " + l2Value);

        assertNull(l1Value, "数据应该已从L1缓存中删除");
        assertNull(l2Value, "数据应该已从L2缓存中删除");
        System.out.println("测试结果: 通过");
    }

    @Test
    void testCacheClear() {
        System.out.println("=== testCacheClear ===");

        // 测试数据
        String cacheName = "testCache";
        String key1 = "clearKey1";
        String key2 = "clearKey2";
        String value1 = "clearValue1";
        String value2 = "clearValue2";

        System.out.println("目标返回: 缓存空间被成功清空");

        // 写入数据到双层缓存
        twoLevelCacheManager.put(cacheName, key1, value1);
        twoLevelCacheManager.put(cacheName, key2, value2);

        System.out.println("已写入测试数据到缓存");

        // 验证数据存在于缓存中
        String result1 = twoLevelCacheManager.get(cacheName, key1, () -> null);
        String result2 = twoLevelCacheManager.get(cacheName, key2, () -> null);
        System.out.println("清空前获取到的值1: " + result1);
        System.out.println("清空前获取到的值2: " + result2);
        assertEquals(value1, result1, "数据1应该存在于缓存中");
        assertEquals(value2, result2, "数据2应该存在于缓存中");

        // 清空缓存空间
        twoLevelCacheManager.clear(cacheName);

        System.out.println("已执行缓存清空操作");

        // 验证数据已从缓存中清空
        String clearedResult1 = twoLevelCacheManager.get(cacheName, key1, () -> null);
        String clearedResult2 = twoLevelCacheManager.get(cacheName, key2, () -> null);
        System.out.println("清空后获取到的值1: " + clearedResult1);
        System.out.println("清空后获取到的值2: " + clearedResult2);

        assertNull(clearedResult1, "数据1应该已从缓存中清空");
        assertNull(clearedResult2, "数据2应该已从缓存中清空");
        System.out.println("测试结果: 通过");
    }

    @Test
    void testNullValueCaching() {
        System.out.println("=== testNullValueCaching ===");

        // 测试数据
        String cacheName = "testCache";
        String key = "nullKey";

        System.out.println("目标返回: 空值被正确缓存，返回null");

        // 清理缓存
        if (caffeineCacheManager.getCache(cacheName) != null) {
            caffeineCacheManager.getCache(cacheName).evict(key);
        }
        if (redisCacheManager.getCache(cacheName) != null) {
            redisCacheManager.getCache(cacheName).evict(key);
        }

        // 通过双层缓存管理器获取null值（模拟数据库中不存在的数据）
        String result = twoLevelCacheManager.get(cacheName, key, () -> null);

        System.out.println("实际返回: " + result);

        // 验证空值是否已缓存（防止缓存穿透）
        Object l1Value = null;
        Object l2Value = null;
        if (caffeineCacheManager.getCache(cacheName) != null) {
            var wrapper = caffeineCacheManager.getCache(cacheName).get(key);
            l1Value = wrapper != null ? wrapper.get() : null;
        }
        if (redisCacheManager.getCache(cacheName) != null) {
            var wrapper = redisCacheManager.getCache(cacheName).get(key);
            l2Value = wrapper != null ? wrapper.get() : null;
        }

        System.out.println("L1缓存空值结果: " + l1Value);
        System.out.println("L2缓存空值结果: " + l2Value);

        assertNull(result, "应该返回null");
        assertNotNull(l1Value, "空值应该已缓存在L1中");
        assertNotNull(l2Value, "空值应该已缓存在L2中");
        System.out.println("测试结果: 通过");
    }
}