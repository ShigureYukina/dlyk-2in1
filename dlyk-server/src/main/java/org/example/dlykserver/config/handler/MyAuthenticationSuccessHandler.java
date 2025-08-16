package org.example.dlykserver.config.handler;


import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dlykserver.contant.Constants;
import org.example.dlykserver.model.TUser;
import org.example.dlykserver.result.R;
import org.example.dlykserver.service.RedisService;
import org.example.dlykserver.service.UserService;
import org.example.dlykserver.util.JSONUtils;
import org.example.dlykserver.util.JWTUtils;
import org.example.dlykserver.util.ResponseUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Resource
    private RedisService redisService;

    @Resource
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //由于禁用了session，我们在登录成功后，需要在服务器保持用户的登录状态，前端下次来访问服务器端的时候，服务器端要知道这个人登录了
        TUser tUser = (TUser) authentication.getPrincipal();

        //1、生成一个jwt字符串
        String userJSON = JSONUtils.toJSON(tUser);
        String jwt = JWTUtils.createJWT(userJSON);

        //2、jwt字符串写入redis
        Integer userId = tUser.getId();
        redisService.setValue(Constants.REDIS_JWT_KEY + userId, jwt); //要设置jwt不同的过期时间，选择记住我是7天过期，否则是30分钟过期

        String rememberMe = request.getParameter("rememberMe"); //true，false，undefined
        if (Boolean.parseBoolean(rememberMe)) {
            redisService.expire(Constants.REDIS_JWT_KEY + userId, Constants.EXPIRE_TIME, TimeUnit.MINUTES);
        } else {
            redisService.expire(Constants.REDIS_JWT_KEY + userId, Constants.DEFAULT_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        R result = R.OK(jwt);

        String resultJSON = JSONUtils.toJSON(result);

        ResponseUtils.write(response, resultJSON);

    }
}
