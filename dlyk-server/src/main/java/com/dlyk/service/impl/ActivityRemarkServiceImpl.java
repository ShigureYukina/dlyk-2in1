package com.dlyk.service.impl;

import com.dlyk.mapper.TActivityRemarkMapper;
import com.dlyk.model.TActivityRemark;
import com.dlyk.query.ActivityRemarkQuery;
import com.dlyk.service.ActivityRemarkService;
import com.dlyk.util.JWTUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

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

}
