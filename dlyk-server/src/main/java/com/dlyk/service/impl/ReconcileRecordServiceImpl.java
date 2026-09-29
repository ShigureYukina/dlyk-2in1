package com.dlyk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dlyk.mapper.TReconcileMapper;
import com.dlyk.model.TReconcileRecord;
import com.dlyk.service.ReconcileRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对账记录查询服务实现
 *
 * <p>这里刻意用 MyBatis-Plus 的条件构造器而不是写 XML：
 * 条件组合（类型可选、按时间倒序、条数上限）随查询需求变化频繁，
 * 用 Wrapper 表达可以避免为每种组合写一条 SQL。
 *
 * @author ShigureYukina
 */
@Service
public class ReconcileRecordServiceImpl implements ReconcileRecordService {

    /** 单次查询条数上限，避免运维接口被用来拖库 */
    private static final int MAX_LIMIT = 500;

    @Resource
    private TReconcileMapper tReconcileMapper;

    @Override
    public List<TReconcileRecord> listRecent(String bizType, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LIMIT);

        LambdaQueryWrapper<TReconcileRecord> wrapper = Wrappers.<TReconcileRecord>lambdaQuery()
                .eq(StringUtils.hasText(bizType), TReconcileRecord::getBizType, bizType)
                .orderByDesc(TReconcileRecord::getCreateTime)
                .orderByDesc(TReconcileRecord::getId)
                // size 已在上面被钳制为正整数，不存在拼接注入风险
                .last("LIMIT " + size);

        return tReconcileMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Long> countGroupByBizType() {
        QueryWrapper<TReconcileRecord> wrapper = Wrappers.<TReconcileRecord>query()
                .select("biz_type AS bizType", "COUNT(*) AS cnt")
                .groupBy("biz_type");

        List<Map<String, Object>> rows = tReconcileMapper.selectMaps(wrapper);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object count = row.get("cnt");
            counts.put(String.valueOf(row.get("bizType")),
                    count == null ? 0L : ((Number) count).longValue());
        }
        return counts;
    }
}
