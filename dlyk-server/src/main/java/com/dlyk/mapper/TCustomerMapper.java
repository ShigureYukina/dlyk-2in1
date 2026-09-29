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

    /**
     * 存活客户总数（单表统计，避免对联查 SQL 做 count）
     */
    long countAlive();

    List<TCustomer> selectCustomerExcel(List<Integer> idList);

    /**
     * 按负责人统计客户数量
     */
    List<NameValue> selectCustomerStatsByOwner();
    
    /**
     * 查询客户详情
     */
    TCustomer selectCustomerDetailById(Integer id);

    /**
     * 逻辑删除客户(deleted 置 1)
     */
    int logicalDeleteById(Integer id);
}