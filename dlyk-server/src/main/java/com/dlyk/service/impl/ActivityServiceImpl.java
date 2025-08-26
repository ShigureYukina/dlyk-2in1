package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TActivityMapper;
import com.dlyk.model.TActivity;
import com.dlyk.query.ActivityQuery;
import com.dlyk.service.ActivityService;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    @Override
    public int saveActivity(ActivityQuery activityQuery) {

        TActivity tActivity = new TActivity();

        //把activityQuery对象里面的属性数据复制到TUser对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(activityQuery, tActivity);

        tActivity.setCreateTime(new Date());//设置创建时间

        Integer userId = JWTUtils.parseUserFromJWT(activityQuery.getToken()).getId();

        tActivity.setCreateBy(userId);//设置创建人

        return tActivityMapper.insertSelective(tActivity);
    }

    @Override
    public TActivity getActivityById(Integer id) {
        return tActivityMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateActivity(ActivityQuery activityQuery) {

        TActivity tActivity = new TActivity();

        //把ActivityQuery对象里面的属性数据复制到TActivity对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(activityQuery, tActivity);

        tActivity.setEditTime(new Date());//设置编辑时间
        Integer userId = JWTUtils.parseUserFromJWT(activityQuery.getToken()).getId();
        tActivity.setEditBy(userId);//设置编辑人

        return tActivityMapper.updateByPrimaryKeySelective(tActivity);
    }

    @Override
    public int deleteActivity(Integer id) {
        return tActivityMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int batchDeleteActivityByIds(List<String> idList) {
        return tActivityMapper.deleteByIds(idList);
    }

    @Override
    public List<TActivity> getOngoingActivity() {
        return tActivityMapper.selectOngoingActivity();
    }
}
