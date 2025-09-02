package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.manager.CustomerManager;
import com.dlyk.mapper.TCustomerMapper;
import com.dlyk.model.TCustomer;
import com.dlyk.query.CustomerQuery;
import com.dlyk.result.CustomerExcel;
import com.dlyk.service.CustomerService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** copy by ShigureYukina,from 2025/8/26-下午10:13 */
@Service
public class CustomerServiceImpl implements CustomerService {
    @Resource
    private CustomerManager customerManager;
    @Resource
    private TCustomerMapper tCustomerMapper;

    @Override
    public Boolean convertCustomer(CustomerQuery customerquery) {
        return customerManager.convertCustomer(customerquery);
    }

    @Override
    public PageInfo<TCustomer> getCustomerByPage(Integer current) {
        //1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        //2.查询
        List<TCustomer> list = this.tCustomerMapper.selectCustomerPage();
        //3.封装分页数据到PageInfo
        PageInfo<TCustomer> info = new PageInfo<>(list);

        return info;
    }


    @Override
    public List<CustomerExcel> getCustomerByExcel(List<Integer> idList) {
        List<CustomerExcel> customerExcelList = new ArrayList<>();
        List<TCustomer> tCustomerList = this.tCustomerMapper.selectCustomerExcel(idList);

        //现在需要把 List<TCustomer> 的数据转换到 List<CustomerExcel>

        tCustomerList.forEach(tCustomer -> {
            CustomerExcel customerExcel = new CustomerExcel();

            customerExcel.setOwnerName(tCustomer.getOwnerDO() != null ? tCustomer.getOwnerDO().getName() : Constants.EMPTY);
            customerExcel.setActivityName(tCustomer.getActivityDO() != null ? tCustomer.getActivityDO().getName() : "");

            if (tCustomer.getClueDO() != null) {
                customerExcel.setFullName(tCustomer.getClueDO().getFullName() != null ? tCustomer.getClueDO().getFullName() : "");
                customerExcel.setPhone(tCustomer.getClueDO().getPhone() != null ? tCustomer.getClueDO().getPhone() : "");
                customerExcel.setWeixin(tCustomer.getClueDO().getWeixin() != null ? tCustomer.getClueDO().getWeixin() : "");
                customerExcel.setQq(tCustomer.getClueDO().getQq() != null ? tCustomer.getClueDO().getQq() : "");
                customerExcel.setEmail(tCustomer.getClueDO().getEmail() != null ? tCustomer.getClueDO().getEmail() : "");
                customerExcel.setAge(tCustomer.getClueDO().getAge() != null ? tCustomer.getClueDO().getAge() : 0);
                customerExcel.setJob(tCustomer.getClueDO().getJob() != null ? tCustomer.getClueDO().getJob() : "");
                customerExcel.setYearIncome(tCustomer.getClueDO().getYearIncome());
                customerExcel.setAddress(tCustomer.getClueDO().getAddress() != null ? tCustomer.getClueDO().getAddress() : "");
            } else {
                // 设置默认值
                customerExcel.setFullName("");
                customerExcel.setPhone("");
                customerExcel.setWeixin("");
                customerExcel.setQq("");
                customerExcel.setEmail("");
                customerExcel.setAge(0);
                customerExcel.setJob("");
                customerExcel.setYearIncome(null);
                customerExcel.setAddress("");
            }

            customerExcel.setNeedLoadName(tCustomer.getNeedLoanDO() != null ? tCustomer.getNeedLoanDO().getTypeValue() : "");
            customerExcel.setProductName(tCustomer.getIntentionProductDO() != null ? tCustomer.getIntentionProductDO().getName() : "");
            customerExcel.setSourceName(tCustomer.getSourceDO() != null ? tCustomer.getSourceDO().getTypeValue() : "");
            customerExcel.setDescription(tCustomer.getDescription());
            customerExcel.setNextContactTime(tCustomer.getNextContactTime());

            customerExcelList.add(customerExcel);
        });

        return customerExcelList;
    }
    
    @Override
    public TCustomer getCustomerDetail(Integer id) {
        return tCustomerMapper.selectCustomerDetailById(id);
    }
}


