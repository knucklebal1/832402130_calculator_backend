package com.fzu.calculator.model.dto;

/**
 * 计算统计响应体（加分项）。
 *
 * @param total           历史记录总条数
 * @param topOperator     使用次数最多的运算符
 * @param topOperatorCount 该运算符出现的次数
 * @param averageResult   所有结果的平均值
 * @param latestAt        最近一次计算时间
 */
public record HistoryStatsResponse(
        boolean success,
        long total,
        String topOperator,
        long topOperatorCount,
        String averageResult,
        String latestAt
) {
}
