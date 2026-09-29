package com.dlyk.config.handler;

import com.dlyk.exception.DuplicateRequestException;
import com.dlyk.result.CodeEnum;
import com.dlyk.result.R;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    DataIntegrityViolationException e;

    @ExceptionHandler
    public R handlerException(Exception e) {
        e.printStackTrace();
        return R.FAIL(e.getMessage());
    }

    @ExceptionHandler
    public R handlerException(AccessDeniedException e) {
        e.printStackTrace();
        return R.FAIL("权限不足");
    }

    @ExceptionHandler
    public R handlerSQLException(DataAccessException e) {
        e.printStackTrace();
        return R.FAIL("数据库操作失败");
    }

    /**
     * 幂等拦截：窗口期内重复提交直接返回 429，不打日志堆栈
     */
    @ExceptionHandler(DuplicateRequestException.class)
    public R handlerDuplicateRequest(DuplicateRequestException e) {
        return R.builder()
                .code(CodeEnum.DUPLICATE_REQUEST.getCode())
                .msg(e.getMessage())
                .build();
    }
}
