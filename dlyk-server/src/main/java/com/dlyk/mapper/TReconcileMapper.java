package com.dlyk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dlyk.model.TReconcileRecord;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 数据一致性对账
 *
 * <p>所有查询都按 {@code MOD(id, shardTotal) = shardIndex} 过滤，
 * 以便在 XXL-Job 分片广播模式下把全表扫描由 N 个执行器并行分担。
 */
public interface TReconcileMapper extends BaseMapper<TReconcileRecord> {

    /** 线索已转换但缺失客户记录的线索ID */
    List<Integer> selectConvertMissingClueIds(@Param("shardIndex") int shardIndex,
                                             @Param("shardTotal") int shardTotal);

    /** 存在重复未删除客户的线索ID */
    List<Integer> selectDuplicateCustomerClueIds(@Param("shardIndex") int shardIndex,
                                                 @Param("shardTotal") int shardTotal);

    /** 线索已逻辑删除但客户仍有效的客户ID */
    List<Integer> selectOrphanCustomerIds(@Param("shardIndex") int shardIndex,
                                          @Param("shardTotal") int shardTotal);

    /** 指定线索中"已转换但缺失客户记录"的线索ID（供事件驱动的增量校验使用） */
    List<Integer> selectConvertMissingClueIdsByIds(@Param("clueIds") List<Integer> clueIds);

    /** 指定线索中存在重复有效客户的线索ID（供事件驱动的增量校验使用） */
    List<Integer> selectDuplicateCustomerClueIdsByIds(@Param("clueIds") List<Integer> clueIds);

    /** 批量写入对账记录 */
    int insertBatch(@Param("list") List<TReconcileRecord> list);

    /** 清理历史对账记录 */
    int deleteBefore(@Param("time") Date time);

    /** 按类型统计最近的差异数量 */
    int countByType(@Param("bizType") String bizType);
}
