package org.example.dlykserver.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 枚举类
 *
 * 数得过来的东西，可以用枚举来定义
 *
 * 一年有12个月，一周有7天，那我们的状态码信息是数得过来的，几十个，最多一两个百个
 */

@AllArgsConstructor
public enum CodeEnum {

    OK(200, "成功"),

    FAIL(500, "失败"),


    TOKEN_IS_EMPTY(901, "请求TOKEN为空"),

    TOKEN_IS_EXPIRED(902, "请求TOKEN已过期"),

    Token_IS_ERROR(903, "请求TOKEN错误"),

    TOKEN_IS_NONE_MATCH(904, "请求TOKEN不匹配"),

    ;

    @Getter
    @Setter
    private int code;

    @Getter
    @Setter
    private String msg;
}
