package com.dlyk.aspect;

import com.dlyk.commons.DataScope;
import com.dlyk.model.TUser;
import com.dlyk.query.BaseQuery;
import com.dlyk.util.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static com.dlyk.constant.Constants.TOKEN_HEADER;

/** copy by ShigureYukina,from 2025/8/19-下午8:26 */
@Aspect
@Component
public class DataScoprAspect {
    @Pointcut(value = "@annotation(com.dlyk.commons.DataScope)")
    public void pointCut() {
        System.out.println("pointCut");
    }

    @Around(value = "pointCut()")
    public Object process(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        DataScope dataScope = methodSignature.getMethod().getAnnotation(DataScope.class);

        String tableAlias = dataScope.tableAlias();
        String tableField = dataScope.tableField();

        // 获取请求参数
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader(TOKEN_HEADER);
        TUser tUser = JWTUtils.parseUserFromJWT(token);
        List<String> roleList = tUser.getRoleList();
        if (!roleList.contains("admin")) {
            Object params = joinPoint.getArgs()[0];
            if (params instanceof BaseQuery) {
                BaseQuery baseQuery = (BaseQuery) params;
                baseQuery.setFilterSQL(" and " + tableAlias + "." + tableField + "=" + tUser.getId());
            }

        }
        System.out.println("目标方法执行前");
        Object result = joinPoint.proceed();
        System.out.println("目标方法执行后");
        return result;
    }
}