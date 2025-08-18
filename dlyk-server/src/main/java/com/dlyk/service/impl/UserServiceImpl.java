package com.dlyk.service.impl;

import com.dlyk.constant.Constants;
import com.dlyk.mapper.TUserMapper;
import com.dlyk.model.TUser;
import com.dlyk.query.UserQuery;
import com.dlyk.service.UserService;
import com.dlyk.util.JWTUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private TUserMapper tUserMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        TUser tUser = tUserMapper.selectByLoginAct(username);
        if (tUser == null) {
            throw new UsernameNotFoundException("登录账号不存在");
        }

        return tUser;
    }

    @Override
    public PageInfo<TUser> getUserByPage(Integer current) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TUser> list = tUserMapper.selectUserByPage();
        // 3.封装分页数据到PageInfo
        PageInfo<TUser> info = new PageInfo<>(list);
        return info;
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
}
