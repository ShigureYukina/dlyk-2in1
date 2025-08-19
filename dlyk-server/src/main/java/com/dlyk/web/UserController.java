package com.dlyk.web;

import com.dlyk.model.TUser;
import com.dlyk.query.UserQuery;
import com.dlyk.result.R;
import com.dlyk.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 获取登录人信息
     *
     * @param authentication
     * @return
     */
    @GetMapping(value = "/api/login/info")
    public R loginInfo(Authentication authentication) {
        TUser tUser = (TUser) authentication.getPrincipal();
        return R.OK(tUser);
    }

    /**
     * 免登录
     *
     * @return
     */
    @GetMapping(value = "/api/login/free")
    public R freeLogin() {
        return R.OK();
    }

    /**
     * 获取用户列表
     *
     * @return
     */
    @GetMapping(value = "/api/users")
    public R UserPage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TUser> pageInfo = userService.getUserByPage(current);
        return R.OK(pageInfo);
    }

    @GetMapping(value = "/api/user/")
    public R userDetail(@RequestParam(value = "id") Integer id) {
        TUser tUser = userService.getUserById(id);
        return R.OK(tUser);
    }

    @PostMapping(value = "/api/user")
    public R saveUser(UserQuery userQuery, @RequestHeader(value = "Authorization") String token) {
        userQuery.setToken(token);
        int result = userService.saveUser(userQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }

    @PutMapping(value = "/api/user")
    public R updateUser(UserQuery userQuery, @RequestHeader(value = "Authorization") String token) {
        userQuery.setToken(token);
        int result = userService.updateUser(userQuery);
        return result >= 1 ? R.OK() : R.FAIL();
    }

    @DeleteMapping(value = "/api/user/{id}")
    public R deleteUser(@PathVariable(value = "id") Integer id) {
        int del = userService.deleteUser(id);
        return del >= 1 ? R.OK() : R.FAIL();
    }

    @DeleteMapping(value = "/api/user/batch")
    public R batchdeleteUser(@RequestParam(value = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        int Batchdel = userService.batchdeleteUserByIds(idList);
        return Batchdel >= 1 ? R.OK() : R.FAIL();
    }
}
