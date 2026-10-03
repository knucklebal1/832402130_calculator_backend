package com.fzu.calculator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 计算器业务参数，对应 application.yml 中的 app.calculator.* 。
 *
 * @param divisionScale       除法保留的小数位数
 * @param maxNestingDepth     允许的最大括号嵌套层数
 * @param maxExpressionLength 表达式最大长度
 */
@ConfigurationProperties(prefix = "app.calculator")
public record CalculatorProperties(
        int divisionScale,
        int maxNestingDepth,
        int maxExpressionLength
) {

    public CalculatorProperties {
        if (divisionScale <= 0) {
            divisionScale = 10;
        }
        if (maxNestingDepth <= 0) {
            maxNestingDepth = 50;
        }
        if (maxExpressionLength <= 0) {
            maxExpressionLength = 200;
        }
    }
}
