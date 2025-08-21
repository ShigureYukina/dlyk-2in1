package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TActivityMapper;
import com.dlyk.model.TActivity;
import com.dlyk.query.ActivityQuery;
import com.dlyk.service.ActivityService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/** copy by ShigureYukina,from 2025/8/20-下午4:18 */
@Service
public class ActivityServiceImpl implements ActivityService {
    @Resource
    private TActivityMapper tActivityMapper;

    @Override
    public PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityquery) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TActivity> list = tActivityMapper.selectActivityByPage(activityquery);
        // 3.封装分页数据到PageInfo
        PageInfo<TActivity> info = new PageInfo<>(list);

        return info;
    }

}
