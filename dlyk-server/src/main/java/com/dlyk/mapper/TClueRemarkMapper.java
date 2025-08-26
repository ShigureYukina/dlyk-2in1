package com.dlyk.mapper;

import com.dlyk.commons.DataScope;
import com.dlyk.model.TActivityRemark;
import com.dlyk.model.TClueRemark;
import com.dlyk.query.ClueRemarkQuery;

import java.util.List;

public interface TClueRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TClueRemark record);

    int insertSelective(TClueRemark record);

    TClueRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TClueRemark record);

    int updateByPrimaryKey(TClueRemark record);

    @DataScope(tableAlias = "tcr", tableField = "create_by")
    List<TActivityRemark> getActivityRemarkPage(ClueRemarkQuery clueRemarkQuery);
}