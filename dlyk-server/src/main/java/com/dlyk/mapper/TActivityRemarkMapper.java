package com.dlyk.mapper;

import com.dlyk.commons.DataScope;
import com.dlyk.model.TActivityRemark;
import com.dlyk.query.ActivityRemarkQuery;

import java.util.List;

public interface TActivityRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TActivityRemark record);

    int insertSelective(TActivityRemark record);

    TActivityRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TActivityRemark record);

    int updateByPrimaryKey(TActivityRemark record);

    @DataScope(tableAlias = "tar", tableField = "create_by")
    List<TActivityRemark> getActivityRemarkPage(ActivityRemarkQuery activityRemarkQuery);
}