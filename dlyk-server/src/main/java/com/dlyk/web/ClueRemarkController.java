package com.dlyk.web;

import com.dlyk.model.TActivityRemark;
import com.dlyk.model.TClueRemark;
import com.dlyk.query.ClueRemarkQuery;
import com.dlyk.result.R;
import com.dlyk.service.ClueRemarkService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/** copy by ShigureYukina,from 2025/8/26-下午6:17 */
@RestController
public class ClueRemarkController {
    @Resource
    private ClueRemarkService clueRemarkService;

    @PostMapping("/api/clue/remark")
    public R addClueRemark(@RequestBody ClueRemarkQuery clueRemarkQuery, @RequestHeader("Authorization") String token) {
        clueRemarkQuery.setToken(token);
        int save = clueRemarkService.addActivityRemark(clueRemarkQuery);
        return save >= 1 ? R.OK() : R.FAIL();
    }

    @GetMapping("/api/clue/remark")
    public R clueRemarkPage(@RequestParam(value = "current", required = false) Integer current,
                                @RequestParam(value = "clueId") Integer clueId) {
        ClueRemarkQuery Query = new ClueRemarkQuery();
        Query.setClueId(clueId);
        if (current == null) {
            current = 1;
        }
        PageInfo<TActivityRemark> pageInfo = clueRemarkService.getClueRemarkPage(current, Query);
        return R.OK(pageInfo);
    }
    
    @GetMapping("/api/clue/remark/{id}")
    public R clueRemarkDetail(@PathVariable("id") Integer id) {
        TClueRemark tClueRemark = clueRemarkService.getClueRemarkById(id);
        return R.OK(tClueRemark);
    }

    @PutMapping("/api/clue/remark")
    public R updateClueRemark(@RequestBody ClueRemarkQuery clueRemarkQuery, @RequestHeader("Authorization") String token) {
        clueRemarkQuery.setToken(token);
        int update = clueRemarkService.updateClueRemark(clueRemarkQuery);
        return update >= 1 ? R.OK() : R.FAIL();
    }
    
    @DeleteMapping("/api/clue/remark/{id}")
    public R deleteClueRemark(@PathVariable("id") Integer id) {
        int delete = clueRemarkService.deleteClueRemark(id);
        return delete >= 1 ? R.OK() : R.FAIL();
    }
}