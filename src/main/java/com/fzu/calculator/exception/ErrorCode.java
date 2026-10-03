package com.fzu.calculator.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。
 *
 * <p>每个错误码绑定一个 HTTP 状态码和一句面向用户的英文提示，
 * 前后端约定：错误响应体统一为 {@code {"success":false,"code":"...","message":"..."}}。
 */
public enum ErrorCode {

    /** 表达式为空 */
    EXPRESSION_EMPTY(HttpStatus.BAD_REQUEST, "Expression must not be empty"),

    /** 表达式超过长度上限 */
    EXPRESSION_TOO_LONG(HttpStatus.BAD_REQUEST, "Expression is too long"),

    /** 出现无法识别的字符 */
    UNSUPPORTED_CHARACTER(HttpStatus.BAD_REQUEST, "Unsupported character in expression"),

    /** 表达式语法错误（括号不匹配、运算符位置错误等） */
    INVALID_EXPRESSION(HttpStatus.BAD_REQUEST, "Invalid expression"),

    /** 除数为 0 */
    DIVIDE_BY_ZERO(HttpStatus.BAD_REQUEST, "Division by zero"),

    /** 结果超出可存储范围 */
    NUMBER_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "Number out of range"),

    /** 请求体格式错误 */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request body"),

    /** 历史记录不存在 */
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "History record not found"),

    /** 请求路径不存在 */
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),

    /** 服务端内部错误 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
