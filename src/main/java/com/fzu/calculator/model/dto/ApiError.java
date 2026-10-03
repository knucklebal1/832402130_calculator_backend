package com.fzu.calculator.model.dto;

/**
 * 统一的错误响应体。
 *
 * <p>示例：{@code {"success":false,"code":"DIVIDE_BY_ZERO","message":"Division by zero"}}
 */
public record ApiError(boolean success, String code, String message) {

    public static ApiError of(String code, String message) {
        return new ApiError(false, code, message);
    }
}
