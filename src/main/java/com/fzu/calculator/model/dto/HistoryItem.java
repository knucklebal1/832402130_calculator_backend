package com.fzu.calculator.model.dto;

import com.fzu.calculator.calculator.BigDecimals;
import com.fzu.calculator.model.entity.CalculationHistory;
import com.fzu.calculator.util.DateTimes;

/**
 * 历史记录列表中的单条数据。
 */
public record HistoryItem(
        Long id,
        String expression,
        String result,
        String createdAt
) {

    public static HistoryItem from(CalculationHistory history) {
        return new HistoryItem(
                history.getId(),
                history.getExpression(),
                BigDecimals.toPlainString(history.getResult()),
                DateTimes.format(history.getCreatedAt())
        );
    }
}
