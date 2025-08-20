package com.dlyk.service;

import com.dlyk.model.TActivity;
import com.github.pagehelper.PageInfo;

/** copy by ShigureYukina,from 2025/8/20-下午4:13 */
public interface ActivityService {
    PageInfo<TActivity> getActivityByPage(Integer current);
}