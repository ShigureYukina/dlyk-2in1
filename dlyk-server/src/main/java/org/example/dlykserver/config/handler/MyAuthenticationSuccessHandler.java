package org.example.dlykserver.config.handler;


import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dlykserver.model.TUser;
import org.example.dlykserver.result.R;
import org.example.dlykserver.service.UserService;
import org.example.dlykserver.util.JSONUtils;
import org.example.dlykserver.util.ResponseUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {


    @Resource
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //由于禁用了session，我们在登录成功后，需要在服务器保持用户的登录状态，前端下次来访问服务器端的时候，服务器端要知道这个人登录了
        TUser tUser = (TUser) authentication.getPrincipal();

        //1、生成一个jwt字符串
        String userJSON = JSONUtils.toJSON(tUser);


        //3、把jwt字符串返回给前端， 向前端返回json数据
        R result = R.OK(tUser);

        //把R对象转成json
        String resultJSON = JSONUtils.toJSON(result);

        //把json写出去，写到浏览器
        ResponseUtils.write(response, resultJSON);
    }
}
