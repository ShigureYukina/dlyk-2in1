package com.dlyk.service;

import com.dlyk.model.TUser;
import com.dlyk.query.UserQuery;
import com.github.pagehelper.PageInfo;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;


public interface UserService extends UserDetailsService {

    PageInfo<TUser> getUserByPage(Integer current);

    TUser getUserById(Integer id);

    int saveUser(UserQuery userQuery);

    int updateUser(UserQuery userQuery);

    int deleteUser(Integer id);

    int batchdeleteUserByIds(List<String> idList);

    List<TUser> getOwnerList();
}
