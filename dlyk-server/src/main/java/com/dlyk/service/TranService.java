package com.dlyk.service;

import com.dlyk.model.TTran;
import com.dlyk.model.TTranHistory;
import com.dlyk.model.TTranRemark;
import com.dlyk.query.TranQuery;
import com.dlyk.query.TranRemarkQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 交易服务接口
 */
public interface TranService {
    
    /**
     * 分页查询交易列表
     */
    PageInfo<TTran> getTranPage(Integer current);
    
    /**
     * 分页查询交易列表（支持条件查询）
     */
    PageInfo<TTran> getTranPage(Integer current, TranQuery tranQuery);
    
    /**
     * 创建交易
     */
    int addTran(TranQuery tranQuery);
    
    /**
     * 查询交易详情
     */
    TTran getTranDetail(Integer id);
    
    /**
     * 更新交易信息
     */
    int updateTran(TranQuery tranQuery);
    
    /**
     * 更新交易阶段
     */
    int updateTranStage(TranQuery tranQuery);
    
    /**
     * 添加交易备注
     */
    int addTranRemark(TranRemarkQuery tranRemarkQuery);
    
    /**
     * 分页查询交易备注
     */
    PageInfo<TTranRemark> getTranRemarkPage(Integer current, Integer tranId);
    
    /**
     * 查询交易历史记录
     */
    List<TTranHistory> getTranHistory(Integer tranId);
}