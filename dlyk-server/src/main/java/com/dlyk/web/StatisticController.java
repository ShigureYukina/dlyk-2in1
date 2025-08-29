package com.dlyk.web;

import com.dlyk.result.BarChartData;
import com.dlyk.result.NameValue;
import com.dlyk.result.R;
import com.dlyk.result.SummaryData;
import com.dlyk.result.TranBarChartData;
import jakarta.annotation.Resource;
// 修改导入的包，使用我们自己的服务类
import com.dlyk.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/28-下午2:59 */

@RestController
public class StatisticController {
    @Resource
    private StatisticsService statisticService;

    @GetMapping(value = "/api/summary/data")
    public R summaryData() {
        SummaryData overviewData = statisticService.loadSummaryData();
        return R.OK(overviewData);
    }
    
    @GetMapping(value = "/api/statistic/funnelChart")
    public R saleFunnelData() {
        List<NameValue> summaryData = statisticService.loadSaleFunnelData();
        return R.OK(summaryData);
    }
    
    @GetMapping(value = "/api/statistic/pieChart")
    public R sourcePieData() {
        List<NameValue> pieData = statisticService.loadSourcePieData();
        return R.OK(pieData);
    }
    
    @GetMapping(value = "/api/statistic/activityBarChart")
    public R activityBarData() {
        List<Integer> barData = statisticService.loadActivityBarData();
        return R.OK(barData);
    }
    
    @GetMapping(value = "/api/statistic/clueBarChart")
    public R clueBarData() {
        BarChartData barData = statisticService.loadClueBarData();
        return R.OK(barData);
    }
    
    @GetMapping(value = "/api/statistic/customerBarChart")
    public R customerBarData() {
        BarChartData barData = statisticService.loadCustomerBarData();
        return R.OK(barData);
    }
    
    @GetMapping(value = "/api/statistic/tranBarChart")
    public R tranBarData() {
        TranBarChartData barData = statisticService.loadTranBarData();
        return R.OK(barData);
    }
}