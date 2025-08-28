package com.dlyk.web;

import com.dlyk.result.NameValue;
import com.dlyk.result.R;
import com.dlyk.result.SummaryData;
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
}