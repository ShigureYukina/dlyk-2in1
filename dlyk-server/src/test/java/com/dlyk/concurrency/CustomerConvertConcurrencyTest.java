package com.dlyk.concurrency;

import com.dlyk.model.TUser;
import com.dlyk.query.CustomerQuery;
import com.dlyk.service.CustomerService;
import com.dlyk.util.JWTUtils;
import com.dlyk.util.JSONUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线索转客户的并发一致性测试
 *
 * <p>直接调用 {@link CustomerService#convertCustomer}，绕过控制器上的
 * {@code @Idempotent} 幂等切面，从而单独验证 {@code CustomerServiceImpl}
 * 中的 Redisson 分布式锁是否真的把同一线索的转换串行化了。
 *
 * <p>验证目标：并发请求下，同一条线索只能产生一条有效客户记录。
 * 这是"防重防脏写"最直接的证据 —— 仅靠"先查状态再更新"的检查后执行（check-then-act），
 * 并发下多个线程会同时读到未转换状态，各自插入一条客户。
 *
 * <p>需要 MySQL 与 Redis（见 docker-compose.yml）。
 *
 * @author ShigureYukina
 */
@SpringBootTest(properties = {"xxl.job.enabled=false", "management.server.port=0"})
class CustomerConvertConcurrencyTest {

    /** 并发线程数 */
    private static final int THREADS = 20;

    @Resource
    private CustomerService customerService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private Integer clueId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "INSERT INTO t_clue (owner_id, full_name, phone, state, source, description, create_time, create_by, deleted) "
                        + "VALUES (1, ?, ?, 1, 1, 'concurrency-test', NOW(), 1, 0)",
                "并发测试线索-" + System.nanoTime(),
                "139" + String.format("%08d", Math.abs(System.nanoTime() % 100000000L)));
        clueId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    @AfterEach
    void tearDown() {
        if (clueId != null) {
            jdbcTemplate.update("DELETE FROM t_customer WHERE clue_id = ?", clueId);
            jdbcTemplate.update("DELETE FROM t_clue WHERE id = ?", clueId);
        }
    }

    @Test
    @DisplayName("20 并发转换同一线索，只应产生一条有效客户记录")
    void concurrentConvertShouldCreateExactlyOneCustomer() throws Exception {
        String token = buildToken();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch readyGate = new CountDownLatch(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejectedByLock = new AtomicInteger();
        AtomicInteger rejectedByState = new AtomicInteger();
        List<String> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            futures.add(pool.submit(() -> {
                readyGate.countDown();
                try {
                    // 所有线程在同一时刻放行，最大化并发碰撞概率
                    startGate.await();
                    CustomerQuery query = new CustomerQuery();
                    query.setClueId(clueId);
                    query.setToken(token);
                    customerService.convertCustomer(query);
                    success.incrementAndGet();
                } catch (Exception e) {
                    String message = String.valueOf(e.getMessage());
                    if (message.contains("正在转换中")) {
                        rejectedByLock.incrementAndGet();
                    } else if (message.contains("已被转换")) {
                        rejectedByState.incrementAndGet();
                    } else {
                        unexpectedErrors.add(e.getClass().getSimpleName() + ": " + message);
                    }
                }
                return null;
            }));
        }

        assertTrue(readyGate.await(10, TimeUnit.SECONDS), "并发线程未在规定时间内就绪");
        startGate.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        Integer activeCustomers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_customer WHERE clue_id = ? AND deleted = 0", Integer.class, clueId);

        System.out.printf("并发转换结果：成功 %d，被分布式锁拒绝 %d，被状态校验拒绝 %d，预期外异常 %d%n",
                success.get(), rejectedByLock.get(), rejectedByState.get(), unexpectedErrors.size());

        assertEquals(1, activeCustomers,
                "并发转换同一线索后，有效客户记录数应为 1，实际为 " + activeCustomers);

        assertEquals(1, success.get(),
                "只应有 1 个请求转换成功，实际成功 " + success.get() + " 个");

        // 关键断言：失败必须来自分布式锁或状态校验。
        // 若这里出现 DuplicateKeyException，说明锁没有生效，
        // 重复写入只是被数据库唯一索引兜住了 —— 那样测试虽然"还是 1 条"，
        // 但掩盖了锁失效，因此必须单独断言。
        assertTrue(unexpectedErrors.isEmpty(),
                "出现了预期外的异常，说明分布式锁未按预期拦截并发：\n" + String.join("\n", unexpectedErrors));

        int rejected = rejectedByLock.get() + rejectedByState.get();
        assertEquals(THREADS - 1, rejected,
                "除 1 个成功请求外，其余请求都应被锁或状态校验拦截，实际拦截 " + rejected + " 个");
    }

    private String buildToken() {
        TUser user = new TUser();
        user.setId(1);
        return JWTUtils.createJWT(JSONUtils.toJSON(user));
    }
}
