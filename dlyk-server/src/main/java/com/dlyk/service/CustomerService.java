package com.dlyk.service;

import com.dlyk.model.TCustomer;
import com.dlyk.query.CustomerQuery;
import com.dlyk.result.CustomerExcel;
import com.github.pagehelper.PageInfo;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/26-下午10:13 */
public interface CustomerService {
	Boolean convertCustomer(CustomerQuery customerquery);

	PageInfo<TCustomer> getCustomerByPage(Integer current);

	List<CustomerExcel> getCustomerByExcel(List<Integer> idList);

}
