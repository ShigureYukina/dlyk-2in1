package com.dlyk.web;

import com.dlyk.model.TActivity;
import com.dlyk.result.R;
import com.dlyk.service.ActivityService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** copy by ShigureYukina,from 2025/8/20-下午4:09 */
@RestController
public class ActivityController {
    @Resource
    private ActivityService activityService;

    @GetMapping(value = "/api/activitys")
    public R UserPage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TActivity> pageInfo = activityService.getActivityByPage(current);
        return R.OK(pageInfo);
    }
}
