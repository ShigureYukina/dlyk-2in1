package com.dlyk.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制注解
 *
 * <p>标注在写操作方法上，由 {@code IdempotentAspect} 通过 Redis 的
 * SET NX EX 实现"同一业务键在窗口期内只允许成功执行一次"，
 * 用于防止用户重复点击、网络重试、消息重复投递造成的重复写入。
 *
 * @author ShigureYukina
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键前缀。留空时取「类名.方法名」。
     */
    String prefix() default "";

    /**
     * 业务键的 SpEL 表达式，用于从入参中提取唯一标识。
     * 例如 {@code #customerquery.clueId}。
     * 留空时退化为对入参做摘要，调用方应尽量显式指定。
     */
    String key() default "";

    /**
     * 幂等窗口，单位秒。窗口内重复请求会被拒绝。
     */
    long timeout() default 60;

    /**
     * 重复请求时返回给调用方的提示信息。
     */
    String message() default "请勿重复提交，请稍后重试";
}
