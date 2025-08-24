package com.dlyk.web;

import com.dlyk.model.TActivity;
import com.dlyk.result.R;
import com.dlyk.query.ActivityQuery;
import com.dlyk.service.ActivityService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Arrays;
import java.util.List;

@RestController
public class ActivityController {
    @Autowired
    private ActivityService activityService;

    @GetMapping(value = "/api/activitys")
    public R ActivityPage(@RequestParam(value = "current", required = false) Integer current,
                          ActivityQuery activityQuery) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TActivity> pageInfo = activityService.getActivityByPage(current, activityQuery);
        return R.OK(pageInfo);
    }

    @PostMapping(value = "/api/activity")
    public R saveUser(ActivityQuery activityQuery, @RequestHeader(value = "Authorization") String token) {
        activityQuery.setToken(token);
        int result = activityService.saveActivity(activityQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }

    @GetMapping(value = "/api/activity/{id}")
    public R getActivityById(@PathVariable("id") Integer id) {
        TActivity activity = activityService.getActivityById(id);
        return activity == null ? R.FAIL() : R.OK(activity);
    }

    @PutMapping(value = "/api/activity")
    public R editActivity(ActivityQuery activityQuery, @RequestHeader(value = "Authorization") String token) {
        activityQuery.setToken(token);
        int result = activityService.updateActivity(activityQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }

    @DeleteMapping(value = "/api/activity/{id}")
    public R deleteActivity(@PathVariable("id") Integer id) {
        int del = activityService.deleteActivity(id);
        return del >= 1 ? R.OK() : R.FAIL();
    }

    @DeleteMapping(value = "/api/activity/batch")
    public R batchDeleteActivity(@RequestParam(value = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        int batchDel = activityService.batchDeleteActivityByIds(idList);
        return batchDel >= 1 ? R.OK() : R.FAIL();
    }
}