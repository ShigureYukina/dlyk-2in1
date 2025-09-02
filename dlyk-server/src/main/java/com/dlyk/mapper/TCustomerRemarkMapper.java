package com.dlyk.mapper;

import com.dlyk.model.TCustomerRemark;

import java.util.List;

public interface TCustomerRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TCustomerRemark record);

    int insertSelective(TCustomerRemark record);

    TCustomerRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TCustomerRemark record);

    int updateByPrimaryKey(TCustomerRemark record);
    
    /**
     * 根据客户ID查询备注列表
     */
    List<TCustomerRemark> selectByCustomerId(Integer customerId);
}