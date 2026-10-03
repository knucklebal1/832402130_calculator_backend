package com.fzu.calculator.model.dto;

/**
 * 清空历史的响应体：{@code {"success":true,"deleted":3}}
 */
public record DeleteAllResponse(boolean success, long deleted) {
}
