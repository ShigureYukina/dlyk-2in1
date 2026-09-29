package com.dlyk.service.impl;

import com.dlyk.constant.ReconcileType;
import com.dlyk.mapper.TReconcileMapper;
import com.dlyk.model.TReconcileRecord;
import com.dlyk.result.ReconcileResult;
import com.dlyk.service.ReconcileService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据一致性对账服务实现
 *
 * <p>分布式锁与幂等把重复写入的概率压到很低，但锁只覆盖加锁路径：
 * 人工改库、历史脏数据、异常中断等情况仍可能留下不一致。
 * 对账任务作为兜底，周期性反向校验"线索 - 客户"的对应关系并留痕，
 * 使不一致可被主动发现，而不是等业务方投诉。
 *
 * @author ShigureYukina
 */
@Service
public class ReconcileServiceImpl implements ReconcileService {

    private static final Logger log = LoggerFactory.getLogger(ReconcileServiceImpl.class);

    /** 单类型最多落库的明细条数，防止异常数据量把对账表打爆 */
    private static final int MAX_PERSIST_PER_TYPE = 500;

    /** 单条 INSERT 的最大行数 */
    private static final int INSERT_BATCH_SIZE = 200;

    /** 对账记录保留天数 */
    private static final int RETENTION_DAYS = 7;

    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    /** 对账差异指标名，供 Prometheus 采集后直接对"数据不一致"建告警 */
    private static final String METRIC_ISSUES = "dlyk.reconcile.issues";

    @Resource
    private TReconcileMapper tReconcileMapper;

    @Resource
    private MeterRegistry meterRegistry;

    @Override
    public ReconcileResult reconcile(int shardIndex, int shardTotal) {
        long start = System.currentTimeMillis();
        List<TReconcileRecord> pending = new ArrayList<>();

        int convertMissing = collect(ReconcileType.CLUE_CONVERT_MISSING,
                tReconcileMapper.selectConvertMissingClueIds(shardIndex, shardTotal),
                "线索已标记为已转换，但不存在有效的客户记录",
                shardIndex, shardTotal, pending);

        int duplicateCustomer = collect(ReconcileType.DUPLICATE_CUSTOMER,
                tReconcileMapper.selectDuplicateCustomerClueIds(shardIndex, shardTotal),
                "同一条线索对应了多条有效客户记录",
                shardIndex, shardTotal, pending);

        int orphanCustomer = collect(ReconcileType.ORPHAN_CUSTOMER,
                tReconcileMapper.selectOrphanCustomerIds(shardIndex, shardTotal),
                "线索已逻辑删除，但客户仍然有效",
                shardIndex, shardTotal, pending);

        int persisted = persist(pending);

        // 仅由 0 号分片执行保留期清理，避免各分片重复删除
        if (shardIndex == 0) {
            purgeExpired();
        }

        ReconcileResult result = ReconcileResult.builder()
                .shardIndex(shardIndex)
                .shardTotal(shardTotal)
                .convertMissingCount(convertMissing)
                .duplicateCustomerCount(duplicateCustomer)
                .orphanCustomerCount(orphanCustomer)
                .totalIssues(convertMissing + duplicateCustomer + orphanCustomer)
                .persistedCount(persisted)
                .costMs(System.currentTimeMillis() - start)
                .build();

        recordIssues(result);
        if (result.getTotalIssues() > 0) {
            log.warn(result.summary());
        } else {
            log.info(result.summary());
        }
        return result;
    }

    /**
     * 把对账差异打成业务指标。
     *
     * <p>日志只能事后翻，指标才能直接配告警：`dlyk_reconcile_issues_total`
     * 一旦增长就说明数据出现不一致，无需等业务方投诉。
     */
    private void recordIssues(ReconcileResult result) {
        incrementIfPositive(ReconcileType.CLUE_CONVERT_MISSING, result.getConvertMissingCount());
        incrementIfPositive(ReconcileType.DUPLICATE_CUSTOMER, result.getDuplicateCustomerCount());
        incrementIfPositive(ReconcileType.ORPHAN_CUSTOMER, result.getOrphanCustomerCount());
    }

    private void incrementIfPositive(String bizType, int count) {
        if (count > 0) {
            meterRegistry.counter(METRIC_ISSUES, "biz_type", bizType).increment(count);
        }
    }

    /**
     * 增量校验：只检查传入的这几条线索。
     *
     * <p>不执行保留期清理，避免高频事件触发时反复 DELETE。
     */
    @Override
    public ReconcileResult reconcileClues(List<Integer> clueIds) {
        long start = System.currentTimeMillis();
        if (clueIds == null || clueIds.isEmpty()) {
            return ReconcileResult.builder()
                    .shardIndex(0).shardTotal(1).costMs(0)
                    .build();
        }

        List<TReconcileRecord> pending = new ArrayList<>();
        int convertMissing = collect(ReconcileType.CLUE_CONVERT_MISSING,
                tReconcileMapper.selectConvertMissingClueIdsByIds(clueIds),
                "线索已标记为已转换，但不存在有效的客户记录",
                0, 1, pending);
        int duplicateCustomer = collect(ReconcileType.DUPLICATE_CUSTOMER,
                tReconcileMapper.selectDuplicateCustomerClueIdsByIds(clueIds),
                "同一条线索对应了多条有效客户记录",
                0, 1, pending);

        int persisted = persist(pending);
        ReconcileResult result = ReconcileResult.builder()
                .shardIndex(0)
                .shardTotal(1)
                .convertMissingCount(convertMissing)
                .duplicateCustomerCount(duplicateCustomer)
                .orphanCustomerCount(0)
                .totalIssues(convertMissing + duplicateCustomer)
                .persistedCount(persisted)
                .costMs(System.currentTimeMillis() - start)
                .build();

        recordIssues(result);
        if (result.getTotalIssues() > 0) {
            log.warn("增量对账发现不一致 | {}", result.summary());
        } else {
            log.debug("增量对账通过 | {}", result.summary());
        }
        return result;
    }

    private int collect(String bizType, List<Integer> ids, String detail,
                        int shardIndex, int shardTotal, List<TReconcileRecord> pending) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        int limit = Math.min(ids.size(), MAX_PERSIST_PER_TYPE);
        for (int i = 0; i < limit; i++) {
            TReconcileRecord record = new TReconcileRecord();
            record.setBizType(bizType);
            record.setBizKey(String.valueOf(ids.get(i)));
            record.setDetail(detail);
            record.setShardIndex(shardIndex);
            record.setShardTotal(shardTotal);
            record.setCreateTime(now);
            pending.add(record);
        }
        if (ids.size() > MAX_PERSIST_PER_TYPE) {
            log.warn("类型 {} 差异共 {} 条，超过落库上限 {}，仅记录前 {} 条",
                    bizType, ids.size(), MAX_PERSIST_PER_TYPE, MAX_PERSIST_PER_TYPE);
        }
        return ids.size();
    }

    private int persist(List<TReconcileRecord> pending) {
        if (pending.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < pending.size(); i += INSERT_BATCH_SIZE) {
            List<TReconcileRecord> batch = pending.subList(i, Math.min(i + INSERT_BATCH_SIZE, pending.size()));
            total += tReconcileMapper.insertBatch(batch);
        }
        return total;
    }

    private void purgeExpired() {
        Date before = new Date(System.currentTimeMillis() - RETENTION_DAYS * MILLIS_PER_DAY);
        int deleted = tReconcileMapper.deleteBefore(before);
        if (deleted > 0) {
            log.info("清理 {} 天前的过期对账记录 {} 条", RETENTION_DAYS, deleted);
        }
    }
}
