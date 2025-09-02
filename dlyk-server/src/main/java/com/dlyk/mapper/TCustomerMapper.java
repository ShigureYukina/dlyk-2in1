package com.dlyk.mapper;

import com.dlyk.model.TCustomer;
import com.dlyk.result.NameValue;

import java.util.List;

public interface TCustomerMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TCustomer record);

    int insertSelective(TCustomer record);

    TCustomer selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TCustomer record);

    int updateByPrimaryKey(TCustomer record);

    List<TCustomer> selectCustomerPage();

    List<TCustomer> selectCustomerExcel(List<Integer> idList);

    /**
     * 按负责人统计客户数量
     */
    List<NameValue> selectCustomerStatsByOwner();
    
    /**
     * 查询客户详情
     */
    TCustomer selectCustomerDetailById(Integer id);
}