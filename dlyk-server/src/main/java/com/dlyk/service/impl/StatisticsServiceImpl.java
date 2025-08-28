package com.dlyk.service.impl;

import com.dlyk.manager.StatisticsManager;
import com.dlyk.result.NameValue;
import com.dlyk.result.SummaryData;
import com.dlyk.service.StatisticsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/28-下午3:14 */
@Service
public class StatisticsServiceImpl implements StatisticsService {
    @Resource
    private StatisticsManager statisticsManager;

    @Override
    public SummaryData loadSummaryData() {
        return statisticsManager.loadSummaryData();
    }

    @Override
    public List<NameValue> loadSaleFunnelData() {
        return statisticsManager.loadSaleFunnelData();
    }
}
