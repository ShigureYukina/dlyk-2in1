package com.dlyk.web;

import com.dlyk.query.ActivityRemarkQuery;
import com.dlyk.result.R;
import com.dlyk.service.ActivityRemarkService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

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
}