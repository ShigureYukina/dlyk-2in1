package com.dlyk.service;

import com.dlyk.model.TActivity;
import com.dlyk.query.ActivityQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ActivityService {
    PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityquery);

    int saveActivity(ActivityQuery activityQuery);

    TActivity getActivityById(Integer id);

    int updateActivity(ActivityQuery activityQuery);

    int deleteActivity(Integer id);

    int batchDeleteActivityByIds(List<String> idList);
}