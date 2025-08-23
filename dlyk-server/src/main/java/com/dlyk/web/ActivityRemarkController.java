package com.dlyk.web;

import com.dlyk.model.TActivityRemark;
import com.dlyk.query.ActivityRemarkQuery;
import com.dlyk.result.R;
import com.dlyk.service.ActivityRemarkService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/** copy by ShigureYukina,from 2025/8/22-下午10:21 */
@RestController
public class ActivityRemarkController {
    @Resource
    private ActivityRemarkService activityRemarkService;

    @PostMapping("/api/activity/remark")
    public R addActivityRemark(@RequestBody ActivityRemarkQuery activityRemarkQuery, @RequestHeader("Authorization") String token) {
        activityRemarkQuery.setToken(token);
        int save = activityRemarkService.addActivityRemark(activityRemarkQuery);
        return save >= 1 ? R.OK() : R.FAIL();

    }

    @GetMapping("/api/activity/remark")
    public R activityRemarkPage(@RequestParam(value = "current", required = false) Integer current,
                                @RequestParam(value = "activityId") Integer activityId) {
        ActivityRemarkQuery Query = new ActivityRemarkQuery();
        Query.setActivityId(activityId);
        if (current == null) {
            current = 1;
        }
        PageInfo<TActivityRemark> pageInfo = activityRemarkService.getActivityRemarkPage(current, Query);
        return R.OK(pageInfo);
    }

    @GetMapping("/api/activity/remark/{id}")
    public R activityRemarkDetail(@PathVariable("id") Integer id) {
        TActivityRemark tactivityRemark = activityRemarkService.getActivityRemarkById(id);
        return R.OK(tactivityRemark);
    }

    @PutMapping("/api/activity/remark")
    public R updateActivityRemark(@RequestBody ActivityRemarkQuery activityRemarkQuery, @RequestHeader("Authorization") String token) {
        activityRemarkQuery.setToken(token);
        int update = activityRemarkService.updateActivityRemark(activityRemarkQuery);
        return update >= 1 ? R.OK() : R.FAIL();
    }
    @DeleteMapping("/api/activity/remark/{id}")
    public R deleteActivityRemark(@PathVariable("id") Integer id) {
        int delete = activityRemarkService.deleteActivityRemark(id);
        return delete >= 1 ? R.OK() : R.FAIL();
    }
}