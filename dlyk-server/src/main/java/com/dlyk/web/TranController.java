package com.dlyk.web;

import com.dlyk.model.TTran;
import com.dlyk.model.TTranHistory;
import com.dlyk.model.TTranRemark;
import com.dlyk.query.TranQuery;
import com.dlyk.query.TranRemarkQuery;
import com.dlyk.result.R;
import com.dlyk.service.TranService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交易控制器
 */
@RestController
public class TranController {
    
    @Resource
    private TranService tranService;
    
    /**
     * 分页查询交易列表
     */
    @GetMapping(value = "/api/trans")
    public R getTranPage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTran> pageInfo = tranService.getTranPage(current);
        return R.OK(pageInfo);
    }
    
    /**
     * 分页查询交易列表（支持条件查询）
     */
    @PostMapping(value = "/api/trans/search")
    public R getTranPageWithQuery(@RequestParam(value = "current", required = false) Integer current,
                                  @RequestBody(required = false) TranQuery tranQuery) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTran> pageInfo = tranService.getTranPage(current, tranQuery);
        return R.OK(pageInfo);
    }
    
    /**
     * 创建交易
     */
    @PostMapping(value = "/api/tran")
    public R addTran(@RequestBody TranQuery tranQuery, @RequestHeader("Authorization") String token) {
        tranQuery.setToken(token);
        int result = tranService.addTran(tranQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }
    
    /**
     * 查询交易详情
     */
    @GetMapping(value = "/api/tran/{id}")
    public R getTranDetail(@PathVariable("id") Integer id) {
        TTran tran = tranService.getTranDetail(id);
        return tran != null ? R.OK(tran) : R.FAIL();
    }
    
    /**
     * 更新交易信息
     */
    @PutMapping(value = "/api/tran")
    public R updateTran(@RequestBody TranQuery tranQuery, @RequestHeader("Authorization") String token) {
        tranQuery.setToken(token);
        int result = tranService.updateTran(tranQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }
    
    /**
     * 更新交易阶段
     */
    @PutMapping(value = "/api/tran/stage")
    public R updateTranStage(@RequestBody TranQuery tranQuery, @RequestHeader("Authorization") String token) {
        tranQuery.setToken(token);
        int result = tranService.updateTranStage(tranQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }
    
    /**
     * 添加交易备注
     */
    @PostMapping(value = "/api/tran/remark")
    public R addTranRemark(@RequestBody TranRemarkQuery tranRemarkQuery, @RequestHeader("Authorization") String token) {
        tranRemarkQuery.setToken(token);
        int result = tranService.addTranRemark(tranRemarkQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }
    
    /**
     * 分页查询交易备注
     */
    @GetMapping(value = "/api/tran/remark")
    public R getTranRemarkPage(@RequestParam(value = "current", required = false) Integer current,
                               @RequestParam(value = "tranId") Integer tranId) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TTranRemark> pageInfo = tranService.getTranRemarkPage(current, tranId);
        return R.OK(pageInfo);
    }
    
    /**
     * 查询交易历史记录
     */
    @GetMapping(value = "/api/tran/history/{tranId}")
    public R getTranHistory(@PathVariable("tranId") Integer tranId) {
        List<TTranHistory> historyList = tranService.getTranHistory(tranId);
        return R.OK(historyList);
    }
}