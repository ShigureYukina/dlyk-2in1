package com.dlyk.service.impl;

import com.dlyk.manager.CustomerManager;
import com.dlyk.query.CustomerQuery;
import com.dlyk.service.CustomerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** copy by ShigureYukina,from 2025/8/26-下午10:13 */
@Service
public class CustomerServiceImpl implements CustomerService {
    @Resource
    private CustomerManager customerManager;

    @Override
    public Boolean convertCustomer(CustomerQuery customerquery) {
        return customerManager.convertCustomer(customerquery);
    }

}
