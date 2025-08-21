package com.dlyk.mapper;

import com.dlyk.commons.DataScope;
import com.dlyk.model.TUser;
import com.dlyk.query.BaseQuery;

import java.util.List;

public interface TUserMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TUser record);

    int insertSelective(TUser record);

    TUser selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TUser record);

    int updateByPrimaryKey(TUser record);

    TUser selectByLoginAct(String username);

    @DataScope(tableAlias = "tu", tableField = "id")
    List<TUser> selectUserByPage(BaseQuery baseQuery);

    TUser selectDetailById(Integer id);

    int deleteByIds(List<String> idList);

    List<TUser> selectByOwner();
}