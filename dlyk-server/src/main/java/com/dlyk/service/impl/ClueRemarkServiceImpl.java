package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TClueRemarkMapper;
import com.dlyk.model.TActivityRemark;
import com.dlyk.model.TClueRemark;
import com.dlyk.query.ClueRemarkQuery;
import com.dlyk.service.ClueRemarkService;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/** copy by ShigureYukina,from 2025/8/26-下午6:21 */
@Service
public class ClueRemarkServiceImpl implements ClueRemarkService {
    @Resource
    private TClueRemarkMapper tClueRemarkMapper;

    @Override
    public int addActivityRemark(ClueRemarkQuery clueRemarkQuery) {
        TClueRemark tClueRemark = new TClueRemark();

        // 把ActivityRemarkQuery对象里的属性数据复制到tClueRemark对象里去
        BeanUtils.copyProperties(clueRemarkQuery, tClueRemark);
        tClueRemark.setCreateTime(new Date()); // 创建时间

        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(clueRemarkQuery.getToken()).getId();
        tClueRemark.setCreateBy(loginUserId); // 创建人

        return tClueRemarkMapper.insertSelective(tClueRemark);
    }

    @Override
    public PageInfo<TActivityRemark> getClueRemarkPage(Integer current, ClueRemarkQuery clueRemarkQuery) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TActivityRemark> list = tClueRemarkMapper.getActivityRemarkPage(clueRemarkQuery);
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }
    
    @Override
    public TClueRemark getClueRemarkById(Integer id) {
        return tClueRemarkMapper.selectByPrimaryKey(id);
    }
    
    @Override
    public int updateClueRemark(ClueRemarkQuery clueRemarkQuery) {
        TClueRemark tClueRemark = new TClueRemark();
        // 把ClueRemarkQuery对象里的属性数据复制到tClueRemark对象里去
        BeanUtils.copyProperties(clueRemarkQuery, tClueRemark);
        tClueRemark.setEditTime(new Date()); // 编辑时间

        // 登录人的id
        Integer loginUserId = JWTUtils.parseUserFromJWT(clueRemarkQuery.getToken()).getId();
        tClueRemark.setEditBy(loginUserId); // 编辑人

        return tClueRemarkMapper.updateByPrimaryKeySelective(tClueRemark);
    }
    
    @Override
    public int deleteClueRemark(Integer id) {
        return tClueRemarkMapper.deleteByPrimaryKey(id);
    }
}