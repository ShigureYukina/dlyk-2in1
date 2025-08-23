package com.dlyk.service;

import com.dlyk.model.TActivityRemark;
import com.dlyk.query.ActivityRemarkQuery;
import com.github.pagehelper.PageInfo;

/** copy by ShigureYukina,from 2025/8/22-下午10:24 */
public interface ActivityRemarkService {
    int addActivityRemark(ActivityRemarkQuery activityRemarkQuery);

    PageInfo<TActivityRemark> getActivityRemarkPage(Integer current,ActivityRemarkQuery activityRemarkQuery);

    TActivityRemark getActivityRemarkById(Integer id);

    int updateActivityRemark(ActivityRemarkQuery activityRemarkQuery);

    int deleteActivityRemark(Integer id);
}
