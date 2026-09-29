package com.dlyk.service;

import com.dlyk.result.ReconcileResult;

import java.util.List;

/**
 * 数据一致性对账服务
 *
 * @author ShigureYukina
 */
public interface ReconcileService {

    /**
     * 对指定分片执行一次对账。
     *
     * @param shardIndex 分片序号，从 0 开始
     * @param shardTotal 分片总数，单机执行时传 1
     * @return 对账结果
     */
    ReconcileResult reconcile(int shardIndex, int shardTotal);

    /**
     * 针对指定线索做一次增量校验。
     *
     * <p>由"客户转化成功"事件异步触发，只校验刚变更的那几条线索，
     * 与 XXL-Job 的全量对账形成"事件驱动 + 周期兜底"的双层防护：
     * 事件驱动让问题在秒级被发现，周期对账兜住漏事件与历史脏数据。
     *
     * @param clueIds 需要校验的线索ID
     * @return 对账结果
     */
    ReconcileResult reconcileClues(List<Integer> clueIds);
}
