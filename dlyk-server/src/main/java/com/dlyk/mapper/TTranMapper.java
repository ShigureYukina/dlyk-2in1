package com.dlyk.mapper;

import com.dlyk.model.TTran;
import com.dlyk.query.TranQuery;
import com.dlyk.result.TranStatsData;
import java.math.BigDecimal;
import java.util.List;

public interface TTranMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTran record);

    int insertSelective(TTran record);

    TTran selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTran record);

    int updateByPrimaryKey(TTran record);
    
    // 查询成功的交易额
    BigDecimal selectSuccessTranAmount();
    
    // 查询总的交易额（包含成功和不成功的）
    BigDecimal selectTotalTranAmount();
    
    // 查询总的交易人数
    int selectTotalTranCount();
    
    // 查询成功的交易人数
    int selectSuccessTranCount();
    
    /**
     * 按负责人统计交易数据
     */
    List<TranStatsData> selectTranStatsByOwner();
    
    /**
     * 分页查询交易列表
     */
    List<TTran> selectTranPage(TranQuery tranQuery);
    
    /**
     * 查询交易详情
     */
    TTran selectTranDetailById(Integer id);
}