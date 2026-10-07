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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 幂等切面
 *
 * <p>基于 Redis SET NX EX 实现：同一条业务键在窗口期内只有第一次请求能拿到
 * 幂等令牌，后续请求直接被拒绝，从而避免重复提交穿透到数据库。
 *
 * <p>令牌的值是本次请求的随机持有者标识，释放时通过 Lua 比对「值相等才删除」：
 * 若业务执行超过了幂等窗口，key 已过期并被下一个请求占住，
 * 第一个请求的异常分支不会误删别人的令牌——与 CacheLockManager 释放锁前
 * 校验持有者是同一条原则。
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

    /** 持有者比对删除：值相等才删，防止超时后误删下一位持有者的令牌 */
    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
            "else return 0 end", Long.class);

    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /** SpEL 表达式编译结果缓存，避免每次请求重复解析 */
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotentKey = buildKey(joinPoint, idempotent);
        // 令牌值 = 本次请求的持有者标识，释放时凭它比对，而不是无差别 delete
        String holderToken = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, holderToken, idempotent.timeout(), TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("重复请求被拦截，幂等键: {}", idempotentKey);
            throw new DuplicateRequestException(idempotent.message());
        }

        log.debug("幂等令牌已获取，幂等键: {}, 窗口: {}s", idempotentKey, idempotent.timeout());
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 业务执行失败则释放令牌，允许合法重试；
            // Lua 比对持有者，窗口已过期且被后续请求占住时不误删
            Long released = stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(idempotentKey), holderToken);
            log.warn("业务执行异常，幂等令牌释放{}，幂等键: {}",
                    released != null && released > 0 ? "成功" : "跳过（已被其他请求持有）", idempotentKey);
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
        // 业务键为空时不能再退化成字面量 "null"：否则窗口期内所有空参请求
        // 共享同一个幂等键，真实的业务报错会被"请勿重复提交"掩盖
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new IllegalArgumentException("幂等业务键为空，请检查 @Idempotent key 表达式取值: " + expressionText);
        }
        return value.toString();
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
