package com.dlyk.web;

import com.dlyk.result.R;
import com.dlyk.service.ReconcileRecordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对账结果查询接口
 *
 * <p>对账任务只负责"发现并记录"，修复动作交给人或后续脚本，
 * 因此必须提供查询入口，否则对账表会变成没人看的黑盒。
 *
 * @author ShigureYukina
 */
@RestController
@RequestMapping("/api/reconcile")
public class ReconcileController {

    @Resource
    private ReconcileRecordService reconcileRecordService;

    /**
     * 查询最近的差异记录。
     *
     * @param bizType 对账类型，可选
     * @param limit   返回条数，默认 50，上限 500
     */
    @GetMapping("/records")
    public R listRecords(@RequestParam(value = "bizType", required = false) String bizType,
                         @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        return R.OK(reconcileRecordService.listRecent(bizType, limit == null ? 50 : limit));
    }

    /**
     * 按类型汇总差异数量。
     */
    @GetMapping("/summary")
    public R summary() {
        return R.OK(reconcileRecordService.countGroupByBizType());
    }
}
