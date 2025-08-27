package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.manager.RedisManager;
import com.dlyk.mapper.TPermissionMapper;
import com.dlyk.mapper.TRoleMapper;
import com.dlyk.mapper.TUserMapper;
import com.dlyk.model.TPermission;
import com.dlyk.model.TRole;
import com.dlyk.model.TUser;
import com.dlyk.query.BaseQuery;
import com.dlyk.query.UserQuery;
import com.dlyk.service.UserService;
import com.dlyk.util.CacheUtils;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private TUserMapper tUserMapper;
    @Resource
    private TRoleMapper tRoleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private TPermissionMapper tPermissionMapper;

    @Resource
    private RedisManager redisManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //这里面去数据库查询一下用户即可
        TUser tUser = tUserMapper.selectByLoginAct(username);
        if (tUser == null) {
            throw new UsernameNotFoundException("登录账号不存在");
        }

        //查询一下用户的角色
        List<TRole> tRoleList = tRoleMapper.selectByUserId(tUser.getId());

        List<String> stringRoleList = new ArrayList<>();

        tRoleList.forEach(tRole -> {
            if (org.springframework.util.StringUtils.hasText(tRole.getRole())) {
                stringRoleList.add(tRole.getRole());
            }
        });
        tUser.setRoleList(stringRoleList);
        //查询用户菜单权限
        List<TPermission> tPermissionList = tPermissionMapper.selectMenuPermissionByUserId(tUser.getId());
        tUser.setMenuPermissionList(tPermissionList);
        //查询用户按钮权限
        List<TPermission> tButtonPermissionList = tPermissionMapper.selectButtonPermissionByUserId(tUser.getId());
        List<String> stringPermissionList = new ArrayList<>();
        tButtonPermissionList.forEach(tPermission -> {
            stringPermissionList.add(tPermission.getCode());
        });
        tUser.setButtonPermissionList(stringPermissionList);

        return tUser;
    }


    @Override
    public PageInfo<TUser> getUserByPage(Integer current) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TUser> list = tUserMapper.selectUserByPage(BaseQuery.builder().build());
        // 3.封装分页数据到PageInfo
        return new PageInfo<>(list);
    }

    @Override
    public TUser getUserById(Integer id) {
        return tUserMapper.selectDetailById(id);
    }

    @Override
    public int saveUser(UserQuery userQuery) {

        TUser tUser = new TUser();

        //把UserQuery对象里面的属性数据复制到TUser对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(userQuery, tUser);

        tUser.setLoginPwd(passwordEncoder.encode(userQuery.getLoginPwd()));

        tUser.setCreateTime(new Date());//设置创建时间
        System.out.println(userQuery.getToken());
        Integer userId = JWTUtils.parseUserFromJWT(userQuery.getToken()).getId();
        System.out.println(userId);
        tUser.setCreateBy(userId);//设置创建人

        return tUserMapper.insertSelective(tUser);
    }

    @Override
    public int updateUser(UserQuery userQuery) {

        TUser tUser = new TUser();

        //把UserQuery对象里面的属性数据复制到TUser对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(userQuery, tUser);
        if (StringUtils.isNotEmpty(userQuery.getLoginPwd())) {
            tUser.setLoginPwd(passwordEncoder.encode(userQuery.getLoginPwd()));
        }

        tUser.setEditTime(new Date());//设置创建时间
        Integer userId = JWTUtils.parseUserFromJWT(userQuery.getToken()).getId();
        tUser.setEditBy(userId);//设置创建人

        return tUserMapper.updateByPrimaryKeySelective(tUser);
    }

    @Override
    public int deleteUser(Integer id) {
        return tUserMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int batchdeleteUserByIds(List<String> idList) {
        return tUserMapper.deleteByIds(idList);
    }

    @Override
    public List<TUser> getOwnerList() {

        return CacheUtils.getCacheData(() -> {
                    Object cacheData = redisManager.getValue(Constants.REDIS_OWNER_KEY);
                    if (cacheData instanceof List) {
                        return (List<TUser>) cacheData;
                    }
                    return null;
                }, () -> {
                    return tUserMapper.selectByOwner();
                },
                (t) -> {
                    redisManager.setValue(Constants.REDIS_OWNER_KEY, t);
                });
    }
}