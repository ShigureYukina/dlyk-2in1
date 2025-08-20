package com.dlyk.commons;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /**
     * 表别名
     */
    public String tableAlias() default "";

    /**
     * 表字段名
     */
    public String tableField() default "";
}
