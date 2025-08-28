package com.dlyk.manager;

import com.dlyk.mapper.TActivityMapper;
import com.dlyk.mapper.TClueMapper;
import com.dlyk.mapper.TCustomerMapper;
import com.dlyk.mapper.TTranMapper;
import com.dlyk.query.ActivityQuery;
import com.dlyk.query.BaseQuery;
import com.dlyk.result.NameValue;
import com.dlyk.result.SummaryData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
}
