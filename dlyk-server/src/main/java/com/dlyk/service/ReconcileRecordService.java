package com.dlyk.service;

import com.dlyk.model.TReconcileRecord;

import java.util.List;
import java.util.Map;

/**
 * 对账记录查询服务（面向运维排查）
 *
 * @author ShigureYukina
 */
public interface ReconcileRecordService {

    /**
     * 查询最近的对账差异记录。
     *
     * @param bizType 对账类型，为空表示不过滤
     * @param limit   返回条数上限
     */
    List<TReconcileRecord> listRecent(String bizType, int limit);

    /**
     * 按对账类型汇总差异数量。
     */
    Map<String, Long> countGroupByBizType();
}
