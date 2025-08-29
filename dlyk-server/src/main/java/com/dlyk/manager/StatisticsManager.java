package com.dlyk.manager;

import com.dlyk.mapper.TActivityMapper;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.mapper.TCustomerMapper;
import com.dlyk.mapper.TTranMapper;
import com.dlyk.query.ActivityQuery;
import com.dlyk.query.BaseQuery;
import com.dlyk.result.BarChartData;
import com.dlyk.result.NameValue;
import com.dlyk.result.SummaryData;
import com.dlyk.result.TranBarChartData;
import com.dlyk.result.TranStatsData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** copy by ShigureYukina,from 2025/8/28-下午3:26 */
@Component
public class StatisticsManager {
    @Resource
    private TActivityMapper tActivityMapper;
    @Resource
    private TClueMapper tClueMapper;
    @Resource
    private TTranMapper tTranMapper;
    @Resource
    private TCustomerMapper tCustomerMapper;

    public SummaryData loadSummaryData() {
        //有效的市场活动总数
        Integer effectiveActivityCount = tActivityMapper.selectOngoingActivity().size();

        //总的市场活动数
        Integer totalActivityCount = tActivityMapper.selectActivityByPage(new ActivityQuery()).size();

        //线索总数
        Integer totalClueCount = tClueMapper.selectClueByPage(new BaseQuery()).size();

        //客户总数
        Integer totalCustomerCount = tCustomerMapper.selectCustomerPage().size();


        BigDecimal successTranAmount = tTranMapper.selectSuccessTranAmount();

        //包含成功和不成功的）
        BigDecimal totalTranAmount = tTranMapper.selectTotalTranAmount();

        return SummaryData.builder()
                .effectiveActivityCount(effectiveActivityCount)
                .totalActivityCount(totalActivityCount)
                .totalClueCount(totalClueCount)
                .totalCustomerCount(totalCustomerCount)
                .successTranAmount(successTranAmount)
                .totalTranAmount(totalTranAmount)
                .build();
    }


    public List<NameValue> loadSaleFunnelData() {
        int clueCount = tClueMapper.selectClueByPage(new BaseQuery()).size();
        int customerCount = tCustomerMapper.selectCustomerPage().size();
        // 修改为查询交易人数和成功交易人数
        int tranCount = tTranMapper.selectTotalTranCount();
        int transSuccessCount = tTranMapper.selectSuccessTranCount();

        return List.of(
                NameValue.builder().name("线索数量").value(String.valueOf(clueCount)).build(),
                NameValue.builder().name("客户数量").value(String.valueOf(customerCount)).build(),
                NameValue.builder().name("交易人数").value(String.valueOf(tranCount)).build(),
                NameValue.builder().name("成功交易人数").value(String.valueOf(transSuccessCount)).build()
        );


    }

    /**
     * 加载线索来源饼图数据
     */
    public List<NameValue> loadSourcePieData() {
        return tClueMapper.selectClueSourceStats();
    }

    /**
     * 加载市场活动柱状图数据
     */
    public List<Integer> loadActivityBarData() {
        return tActivityMapper.selectActivityStatsByMonth();
    }

    /**
     * 加载线索柱状图数据
     */
    public BarChartData loadClueBarData() {
        List<NameValue> stats = tClueMapper.selectClueStatsByOwner();
        return convertToBarChartData(stats);
    }

    /**
     * 加载客户柱状图数据
     */
    public BarChartData loadCustomerBarData() {
        List<NameValue> stats = tCustomerMapper.selectCustomerStatsByOwner();
        return convertToBarChartData(stats);
    }

    /**
     * 加载交易柱状图数据
     */
    public TranBarChartData loadTranBarData() {
        List<TranStatsData> stats = tTranMapper.selectTranStatsByOwner();
        return convertToTranBarChartData(stats);
    }

    /**
     * 转换为柱状图数据格式
     */
    private BarChartData convertToBarChartData(List<NameValue> stats) {
        BarChartData result = new BarChartData();
        List<String> xData = new ArrayList<>();
        List<Integer> yData = new ArrayList<>();

        for (NameValue stat : stats) {
            xData.add(stat.getName());
            yData.add(Integer.parseInt(stat.getValue()));
        }

        result.setX(xData);
        result.setY(yData);
        return result;
    }

    /**
     * 转换为交易柱状图数据格式
     */
    private TranBarChartData convertToTranBarChartData(List<TranStatsData> stats) {
        TranBarChartData result = new TranBarChartData();
        List<String> xData = new ArrayList<>();
        List<Integer> y1Data = new ArrayList<>();
        List<Integer> y2Data = new ArrayList<>();

        for (TranStatsData stat : stats) {
            xData.add(stat.getOwnerName());
            y1Data.add(stat.getTotalTranCount());
            y2Data.add(stat.getSuccessTranCount());
        }

        result.setX(xData);
        result.setY1(y1Data);
        result.setY2(y2Data);
        return result;
    }
}
