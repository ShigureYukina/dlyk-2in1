package org.example.dlykserver.config.fitter;


import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dlykserver.contant.Constants;
import org.example.dlykserver.model.TUser;
import org.example.dlykserver.result.CodeEnum;
import org.example.dlykserver.result.R;
import org.example.dlykserver.service.RedisService;
import org.example.dlykserver.util.JSONUtils;
import org.example.dlykserver.util.JWTUtils;
import org.example.dlykserver.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenVerifyFilter extends OncePerRequestFilter {

    @Resource
    private RedisService redisService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (requestURI.equals(Constants.LOGIN_URI)) {
            //登录的时候还没有生成jwt，此时不需要验证jwt，直接放行即可，可以让filter链继续执行，执行下一个filter
            filterChain.doFilter(request, response);
        } else {
            //拿到前端传过来的jwt（token），这个jwt一般都通过header传过来
            String token = request.getHeader("Authorization");
            if (!StringUtils.hasText(token)) {
                R result = R.FAIL(CodeEnum.TOKEN_IS_EMPTY);
                String resultJSON = JSONUtils.toJSON(result);
                ResponseUtils.write(response, resultJSON);
                return;
            }

            TUser tUser = JWTUtils.parseUserFromJWT(token);
            String redisToken = (String) redisService.getValue(Constants.REDIS_JWT_KEY + tUser.getId());


            if (!StringUtils.hasText(redisToken)) {
                //token 验证未通过
                R result = R.FAIL(CodeEnum.TOKEN_IS_EXPIRED);

                //把R对象转成json
                String resultJSON = JSONUtils.toJSON(result);

                //把json写出去，写到浏览器
                ResponseUtils.write(response, resultJSON);
                return;
            }
            if (!token.equals(redisToken)) {
                //token传过来的和redis里面的不一致，那不合法
                R result = R.FAIL(CodeEnum.TOKEN_IS_NONE_MATCH);
                String resultJSON = JSONUtils.toJSON(result);
                ResponseUtils.write(response, resultJSON);
                return;
            }
            if (!JWTUtils.verifyJWT(token)) {
                //token 验证未通过
                R result = R.FAIL(CodeEnum.Token_IS_ERROR);
                String resultJSON = JSONUtils.toJSON(result);
                ResponseUtils.write(response, resultJSON);
                return;
            }


            //都验证通过了，没有问题了，需要告诉spring security框架，这样spring security框架才知道该jwt是已经登录过的
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(tUser, tUser.getPassword(), tUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            //下面就是filter链继续执行，执行下一个filter
            filterChain.doFilter(request, response);
        }
    }

}
