package com.dlyk.mapper;

import com.dlyk.commons.DataScope;
import com.dlyk.model.TActivity;
import com.dlyk.query.ActivityQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TActivityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TActivity record);

    int insertSelective(TActivity record);

    TActivity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TActivity record);

    int updateByPrimaryKey(TActivity record);

    @DataScope(tableAlias = "ta", tableField = "owner_id")
    List<TActivity> selectActivityByPage(ActivityQuery query);

    int deleteByIds(@Param("idList") List<String> idList);
}