package com.dlyk.exception;

import com.dlyk.result.CodeEnum;

/**
 * 重复请求异常
 *
 * <p>由幂等切面在检测到窗口期内重复提交时抛出，经全局异常处理器
 * 转换为 {@link CodeEnum#DUPLICATE_REQUEST} 返回给调用方。
 *
 * @author ShigureYukina
 */
public class DuplicateRequestException extends RuntimeException {

    public DuplicateRequestException(String message) {
        super(message);
    }
}
