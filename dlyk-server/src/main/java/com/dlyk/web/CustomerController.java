package com.dlyk.web;

import com.alibaba.excel.EasyExcel;
import com.dlyk.model.TCustomer;
import com.dlyk.query.CustomerQuery;
import com.dlyk.query.CustomerRemarkQuery;
import com.dlyk.query.TranQuery;
import com.dlyk.result.CustomerExcel;
import com.dlyk.result.R;
import com.dlyk.service.CustomerService;
import com.dlyk.service.CustomerRemarkService;
import com.dlyk.service.TranService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** copy by ShigureYukina,from 2025/8/26-下午10:08 */
@RestController
public class CustomerController {
    @Resource
    private CustomerService customerService;
    
    @Resource
    private CustomerRemarkService customerRemarkService;
    
    @Resource
    private TranService tranService;

    @PostMapping(value = "/api/clue/customer/")
    public R convertCustomer(@RequestBody CustomerQuery customerquery, @RequestHeader("Authorization") String token) {
        customerquery.setToken(token);
        Boolean convert = customerService.convertCustomer(customerquery);
        return convert ? R.OK() : R.FAIL();
    }
    
    @GetMapping(value = "/api/customers")
    public R customerPage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TCustomer> pageInfo = customerService.getCustomerByPage(current);
        return R.OK(pageInfo);
    }
    
    @GetMapping(value = "/api/customer/{id}")
    public R getCustomerDetail(@PathVariable("id") Integer id) {
        TCustomer customer = customerService.getCustomerDetail(id);
        return customer != null ? R.OK(customer) : R.FAIL();
    }
    
    @PostMapping(value = "/api/customer/remark")
    public R addCustomerRemark(@RequestBody CustomerRemarkQuery customerRemarkQuery, @RequestHeader("Authorization") String token) {
        customerRemarkQuery.setToken(token);
        int save = customerRemarkService.addCustomerRemark(customerRemarkQuery);
        return save >= 1 ? R.OK() : R.FAIL();
    }
    
    @GetMapping(value = "/api/customer/remark")
    public R getCustomerRemarkList(@RequestParam(value = "current", required = false) Integer current,
                                   @RequestParam(value = "customerId") Integer customerId) {
        if (current == null) {
            current = 1;
        }
        PageInfo pageInfo = customerRemarkService.getCustomerRemarkPage(current, customerId);
        return R.OK(pageInfo);
    }
    
    @PostMapping(value = "/api/customer/{id}/transaction")
    public R createTransaction(@PathVariable("id") Integer customerId, 
                              @RequestBody TranQuery tranQuery, 
                              @RequestHeader("Authorization") String token) {
        tranQuery.setCustomerId(customerId);
        tranQuery.setToken(token);
        int result = tranService.addTran(tranQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }


    @GetMapping(value = "/api/exportExcel")
    public void exportExcel(@RequestParam(value = "ids", required = false) String ids, HttpServletResponse response) throws Exception {
        // 在文件名中添加当前时间
        String currentTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "客户数据_" + currentTime + ".xlsx";
        // 浏览器兼容处理，进行URL编码
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        // 设置Content-Type为Excel MIME类型
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 修复：只使用编码后的文件名，避免在header中直接使用中文字符
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        // 获取数据
        List<CustomerExcel> dataList;
        // 如果传入了ids，则导出指定的数据
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        dataList = customerService.getCustomerByExcel(idList);


        // 写出Excel
        EasyExcel.write(response.getOutputStream(), CustomerExcel.class)
                .sheet("客户数据")
                .doWrite(dataList);
    }

}
