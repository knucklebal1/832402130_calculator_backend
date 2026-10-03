package com.fzu.calculator.exception;

/**
 * 业务异常。
 *
 * <p>计算模块、历史模块在遇到"可预期的错误"时统一抛出本异常，
 * 由 {@link GlobalExceptionHandler} 转换成规范的 JSON 错误响应。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
