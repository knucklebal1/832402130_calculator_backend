package com.fzu.calculator.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 计算请求体。
 *
 * <p>示例：{@code {"expression":"(1+2)*3"}}
 */
public record CalculateRequest(
        @NotBlank(message = "Expression must not be empty")
        @Size(max = 200, message = "Expression is too long (max 200 characters)")
        String expression
) {
}
