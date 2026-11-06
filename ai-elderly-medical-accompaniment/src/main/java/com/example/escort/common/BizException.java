package com.example.escort.common;

import lombok.Getter;

/**
 * 业务异常
 * 仅用于可预期的业务逻辑错误（状态冲突、权限不足等），
 * 由 GlobalExceptionHandler 统一捕获处理。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}