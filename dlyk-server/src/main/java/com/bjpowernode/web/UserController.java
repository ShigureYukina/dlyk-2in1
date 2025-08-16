package com.bjpowernode.web;

import com.bjpowernode.model.TUser;
import com.bjpowernode.query.UserQuery;
import com.bjpowernode.result.R;
import com.bjpowernode.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        TUser tUser = (TUser)authentication.getPrincipal();
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

    @GetMapping(value = "/api/users")
    public R userPage(@RequestParam(value = "current", required = false) Integer current) {
        //required = false 表示参数可以传，也可以不传；
        //required = true 表示参数必须要传，不传会报错；
        if (current == null) {
            current = 1;
        }
        PageInfo<TUser> pageInfo = userService.getUserByPage(current);
        return R.OK(pageInfo);
    }

    @GetMapping(value = "/api/user/{id}")
    public R userDetail(@PathVariable(value = "id") Integer id) {
        TUser tUser = userService.getUserById(id);
        return R.OK(tUser);
    }

    @PostMapping(value = "/api/user")
    public R addUser(UserQuery userQuery) {
        int save = userService.saveUser(userQuery);
        return save >= 1 ? R.OK() : R.FAIL();
    }

}
