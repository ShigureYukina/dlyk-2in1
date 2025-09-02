package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TCustomerRemarkMapper;
import com.dlyk.model.TCustomerRemark;
import com.dlyk.query.CustomerRemarkQuery;
import com.dlyk.service.CustomerRemarkService;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 客户备注服务实现类
 */
@Service
public class CustomerRemarkServiceImpl implements CustomerRemarkService {
    
    @Resource
    private TCustomerRemarkMapper tCustomerRemarkMapper;
    
    @Override
    public int addCustomerRemark(CustomerRemarkQuery customerRemarkQuery) {
        TCustomerRemark tCustomerRemark = new TCustomerRemark();
        
        // 把CustomerRemarkQuery对象里的属性数据复制到tCustomerRemark对象里去
        BeanUtils.copyProperties(customerRemarkQuery, tCustomerRemark);
        tCustomerRemark.setCreateTime(new Date()); // 创建时间
        
        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(customerRemarkQuery.getToken()).getId();
        tCustomerRemark.setCreateBy(loginUserId); // 创建人
        
        return tCustomerRemarkMapper.insertSelective(tCustomerRemark);
    }
    
    @Override
    public PageInfo<TCustomerRemark> getCustomerRemarkPage(Integer current, Integer customerId) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TCustomerRemark> list = tCustomerRemarkMapper.selectByCustomerId(customerId);
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }
    
    @Override
    public TCustomerRemark getCustomerRemarkById(Integer id) {
        return tCustomerRemarkMapper.selectByPrimaryKey(id);
    }
    
    @Override
    public int updateCustomerRemark(CustomerRemarkQuery customerRemarkQuery) {
        TCustomerRemark tCustomerRemark = new TCustomerRemark();
        // 把CustomerRemarkQuery对象里的属性数据复制到tCustomerRemark对象里去
        BeanUtils.copyProperties(customerRemarkQuery, tCustomerRemark);
        tCustomerRemark.setEditTime(new Date()); // 编辑时间
        
        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(customerRemarkQuery.getToken()).getId();
        tCustomerRemark.setEditBy(loginUserId); // 编辑人
        
        return tCustomerRemarkMapper.updateByPrimaryKeySelective(tCustomerRemark);
    }
    
    @Override
    public int deleteCustomerRemark(Integer id) {
        // 逻辑删除
        TCustomerRemark tCustomerRemark = new TCustomerRemark();
        tCustomerRemark.setId(id);
        tCustomerRemark.setDeleted(1);
        return tCustomerRemarkMapper.updateByPrimaryKeySelective(tCustomerRemark);
    }
}