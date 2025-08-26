package com.dlyk.web;

import com.dlyk.query.CustomerQuery;
import com.dlyk.result.R;
import com.dlyk.service.CustomerService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** copy by ShigureYukina,from 2025/8/26-下午10:08 */
@RestController
public class CustomerController {
    @Resource
    private CustomerService customerService;

    @PostMapping(value = "/api/clue/customer/")
    public R convertCustomer(@RequestBody CustomerQuery customerquery, @RequestHeader("Authorization") String token) {
        customerquery.setToken(token);
        Boolean convert = customerService.convertCustomer(customerquery);
        return convert ? R.OK() : R.FAIL();
    }
}
