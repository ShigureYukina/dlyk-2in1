package com.dlyk.constant;

/**
 * 对账类型
 *
 * @author ShigureYukina
 */
public final class ReconcileType {

    /** 线索已标记为"已转换"，但找不到对应的客户记录 —— 转换过程丢失 */
    public static final String CLUE_CONVERT_MISSING = "CLUE_CONVERT_MISSING";

    /** 同一条线索对应了多条未删除客户 —— 并发转换造成的重复写入 */
    public static final String DUPLICATE_CUSTOMER = "DUPLICATE_CUSTOMER";

    /** 线索已逻辑删除，但客户仍然有效 —— 删除未级联 */
    public static final String ORPHAN_CUSTOMER = "ORPHAN_CUSTOMER";

    private ReconcileType() {
    }
}
