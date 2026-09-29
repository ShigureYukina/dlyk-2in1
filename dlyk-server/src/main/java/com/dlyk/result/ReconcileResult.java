package com.dlyk.result;

import lombok.Builder;
import lombok.Data;

/**
 * 对账结果
 *
 * @author ShigureYukina
 */
@Data
@Builder
public class ReconcileResult {

    /** 当前执行分片序号 */
    private int shardIndex;

    /** 分片总数 */
    private int shardTotal;

    /** 线索已转换但缺失客户 的条数 */
    private int convertMissingCount;

    /** 存在重复有效客户 的线索条数 */
    private int duplicateCustomerCount;

    /** 线索已删除但客户仍有效 的条数 */
    private int orphanCustomerCount;

    /** 差异总数 */
    private int totalIssues;

    /** 实际落库的明细条数（超过上限会被截断） */
    private int persistedCount;

    /** 耗时（毫秒） */
    private long costMs;

    public String summary() {
        return String.format(
                "分片[%d/%d] 对账完成：转换缺失 %d，重复客户 %d，孤儿客户 %d，合计差异 %d，落库 %d 条，耗时 %dms",
                shardIndex, shardTotal, convertMissingCount, duplicateCustomerCount,
                orphanCustomerCount, totalIssues, persistedCount, costMs);
    }
}
