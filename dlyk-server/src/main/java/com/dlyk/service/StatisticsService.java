package com.dlyk.service;

import com.dlyk.result.BarChartData;
import com.dlyk.result.NameValue;
import com.dlyk.result.SummaryData;
import com.dlyk.result.TranBarChartData;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/28-下午3:14 */

public interface StatisticsService {
    /**
     * 加载统计概览数据
     */
    SummaryData loadSummaryData();

    /**
     * 加载销售漏斗图数据
     */
    List<NameValue> loadSaleFunnelData();

    /**
     * 加载线索来源饼图数据
     */
    List<NameValue> loadSourcePieData();

    /**
     * 加载市场活动柱状图数据
     */
    List<Integer> loadActivityBarData();

    /**
     * 加载线索柱状图数据
     */
    BarChartData loadClueBarData();

    /**
     * 加载客户柱状图数据
     */
    BarChartData loadCustomerBarData();

    /**
     * 加载交易柱状图数据
     */
    TranBarChartData loadTranBarData();
}