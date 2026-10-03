package com.fzu.calculator.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 计算请求体。
 *
 * <p>示例：{@code {"expression":"sin(30)+1","angleMode":"DEG"}}
 *
 * @param expression 待计算的表达式
 * @param angleMode  三角函数角度单位，"DEG"（角度，默认）或 "RAD"（弧度）；
 *                   不传或传入其它值时按角度制处理
 */
public record CalculateRequest(
        @NotBlank(message = "Expression must not be empty")
        @Size(max = 200, message = "Expression is too long (max 200 characters)")
        String expression,

        String angleMode
) {

    /** 是否按角度制计算三角函数。 */
    public boolean angleInDegrees() {
        return angleMode == null || !"RAD".equalsIgnoreCase(angleMode.trim());
    }
}
