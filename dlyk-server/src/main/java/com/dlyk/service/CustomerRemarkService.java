package com.dlyk.service;

import com.dlyk.model.TCustomerRemark;
import com.dlyk.query.CustomerRemarkQuery;
import com.github.pagehelper.PageInfo;

/**
 * 客户备注服务接口
 */
public interface CustomerRemarkService {
    
    int addCustomerRemark(CustomerRemarkQuery customerRemarkQuery);
    
    PageInfo<TCustomerRemark> getCustomerRemarkPage(Integer current, Integer customerId);
    
    TCustomerRemark getCustomerRemarkById(Integer id);
    
    int updateCustomerRemark(CustomerRemarkQuery customerRemarkQuery);
    
    int deleteCustomerRemark(Integer id);
}