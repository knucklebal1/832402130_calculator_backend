package com.fzu.calculator.model.dto;

import com.fzu.calculator.calculator.BigDecimals;
import com.fzu.calculator.model.entity.CalculationHistory;
import com.fzu.calculator.util.DateTimes;

/**
 * 计算成功响应体。
 *
 * <p>示例：
 * {@code {"success":true,"id":4,"expression":"(1+2)*3","result":"9","createdAt":"2026-10-05 10:22:00"}}
 *
 * <p>result 以字符串返回，避免前端 JavaScript 处理大数时丢失精度。
 */
public record CalculateResponse(
        boolean success,
        Long id,
        String expression,
        String result,
        String createdAt
) {

    public static CalculateResponse from(CalculationHistory history) {
        return new CalculateResponse(
                true,
                history.getId(),
                history.getExpression(),
                BigDecimals.toPlainString(history.getResult()),
                DateTimes.format(history.getCreatedAt())
        );
    }
}
