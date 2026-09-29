package com.dlyk.aspect;

import com.dlyk.annotation.Idempotent;
import com.dlyk.exception.DuplicateRequestException;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 幂等切面
 *
 * <p>基于 Redis SET NX EX 实现：同一条业务键在窗口期内只有第一次请求能拿到
 * 幂等令牌，后续请求直接被拒绝，从而避免重复提交穿透到数据库。
 *
 * <p>若业务方法执行抛出异常，幂等键会被立即释放，允许调用方重试，
 * 避免"第一次失败后整个窗口期都无法重试"的问题。
 *
 * @author ShigureYukina
 */
@Aspect
@Component
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    private static final String IDEMPOTENT_KEY_PREFIX = "dlyk:idempotent:";

    /** 不参与幂等键计算的参数名（请求级凭证，每次请求都不同） */
    private static final String[] IGNORED_PARAM_NAMES = {"token", "authorization"};

    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /** SpEL 表达式编译结果缓存，避免每次请求重复解析 */
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotentKey = buildKey(joinPoint, idempotent);

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", idempotent.timeout(), TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("重复请求被拦截，幂等键: {}", idempotentKey);
            throw new DuplicateRequestException(idempotent.message());
        }

        log.debug("幂等令牌已获取，幂等键: {}, 窗口: {}s", idempotentKey, idempotent.timeout());
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 业务执行失败则释放令牌，允许合法重试
            stringRedisTemplate.delete(idempotentKey);
            log.warn("业务执行异常，已释放幂等令牌，幂等键: {}", idempotentKey);
            throw e;
        }
    }

    /**
     * 构造幂等键：固定前缀 + 业务前缀 + 业务唯一标识。
     */
    private String buildKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String prefix = StringUtils.hasText(idempotent.prefix())
                ? idempotent.prefix()
                : joinPoint.getTarget().getClass().getSimpleName() + "." + joinPoint.getSignature().getName();

        String businessKey = StringUtils.hasText(idempotent.key())
                ? evaluateSpel(idempotent.key(), joinPoint)
                : digestArguments(joinPoint);

        return IDEMPOTENT_KEY_PREFIX + prefix + ":" + businessKey;
    }

    private String evaluateSpel(String expressionText, ProceedingJoinPoint joinPoint) {
        Expression expression = expressionCache.computeIfAbsent(expressionText, SPEL_PARSER::parseExpression);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());
        String[] parameterNames = new DefaultParameterNameDiscoverer().getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        Object value = expression.getValue(context);
        return value == null ? "null" : value.toString();
    }

    /**
     * 未显式指定业务键时，对入参做摘要作为兜底。
     * 会剔除 token 这类每次请求都变化的凭证参数。
     */
    private String digestArguments(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = new DefaultParameterNameDiscoverer().getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        StringBuilder raw = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            String name = (parameterNames != null && i < parameterNames.length) ? parameterNames[i] : ("arg" + i);
            if (isIgnored(name)) {
                continue;
            }
            raw.append(name).append('=').append(args[i]).append(';');
        }
        return DigestUtils.md5DigestAsHex(raw.toString().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isIgnored(String parameterName) {
        for (String ignored : IGNORED_PARAM_NAMES) {
            if (ignored.equalsIgnoreCase(parameterName)) {
                return true;
            }
        }
        return false;
    }
}
