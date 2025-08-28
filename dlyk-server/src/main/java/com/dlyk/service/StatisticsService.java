package com.dlyk.service;

import com.dlyk.result.NameValue;
import com.dlyk.result.SummaryData;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/28-下午3:14 */

public interface StatisticsService {
    SummaryData loadSummaryData();

    List<NameValue> loadSaleFunnelData();
}