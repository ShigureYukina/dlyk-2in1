package org.example.dlykserver.service.impl;

import org.example.dlykserver.mapper.TUserMapper;
import org.example.dlykserver.model.TUser;
import org.example.dlykserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** copy by ShigureYukina,from 2025/8/15-涓嬪崍2:53 */
@Service
public class UserServiceimpl implements UserService {
    @Autowired
    private TUserMapper tUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TUser tUser = tUserMapper.selectByLoginAct(username);
        if (tUser == null) {
            throw new UsernameNotFoundException("登录账号不存在");
        }
        return tUser;
    }
}