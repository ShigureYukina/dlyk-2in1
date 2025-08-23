package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TActivityRemarkMapper;
import com.dlyk.model.TActivityRemark;
import com.dlyk.query.ActivityRemarkQuery;
import com.dlyk.service.ActivityRemarkService;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/** copy by ShigureYukina,from 2025/8/22-下午10:27 */
@Service
public class ActivityRemarkServiceImpl implements ActivityRemarkService {
    @Resource
    private TActivityRemarkMapper tActivityRemarkMapper;

    @Override
    public int addActivityRemark(ActivityRemarkQuery activityRemarkQuery) {
        TActivityRemark tActivityRemark = new TActivityRemark();

        // 把ActivityRemarkQuery对象里的属性数据复制到TActivityRemark对象里去
        BeanUtils.copyProperties(activityRemarkQuery, tActivityRemark);
        tActivityRemark.setCreateTime(new Date()); // 创建时间

        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(activityRemarkQuery.getToken()).getId();
        tActivityRemark.setCreateBy(loginUserId); // 创建人

        return tActivityRemarkMapper.insertSelective(tActivityRemark);
    }

    @Override
    public PageInfo<TActivityRemark> getActivityRemarkPage(Integer current, ActivityRemarkQuery activityRemarkQuery) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TActivityRemark> list = tActivityRemarkMapper.getActivityRemarkPage(activityRemarkQuery);
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }

    @Override
    public TActivityRemark getActivityRemarkById(Integer id) {
        return tActivityRemarkMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateActivityRemark(ActivityRemarkQuery activityRemarkQuery) {
        TActivityRemark tActivityRemark = new TActivityRemark();

        //把tActivityRemarkQuery对象里面的属性数据复制到tActivityRemark对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(activityRemarkQuery, tActivityRemark);

        tActivityRemark.setEditTime(new Date());//设置编辑时间
        Integer userId = JWTUtils.parseUserFromJWT(activityRemarkQuery.getToken()).getId();
        tActivityRemark.setEditBy(userId);//设置编辑人

        return tActivityRemarkMapper.updateByPrimaryKeySelective(tActivityRemark);
    }

    @Override
    public int deleteActivityRemark(Integer id) {
        TActivityRemark tActivityRemark = new TActivityRemark();
        tActivityRemark.setId(id);
        tActivityRemark.setDeleted(1);
        return tActivityRemarkMapper.updateByPrimaryKeySelective(tActivityRemark);
    }

}