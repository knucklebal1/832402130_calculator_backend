package com.fzu.calculator.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运算符统计的单元测试。
 *
 * <p>重点验证"一元正负号不算运算符"这一条，它是统计是否有意义的关键。
 */
class OperatorAnalyzerTest {

    private static Map<String, Long> count(String expression) {
        return OperatorAnalyzer.countUsages(expression);
    }

    @ParameterizedTest(name = "{0} -> {1}={2}")
    @CsvSource({
            "1+2*3,      +, 1",
            "1+2*3,      *, 1",
            "10-3,       -, 1",
            "2^10,       ^, 1",
            "5!,         !, 1",
            "50%,        %, 1",
            "sin(pi/2),  /, 1",
            "sqrt(2)+1,  +, 1",
            "2^3^2,      ^, 2"
    })
    void shouldCountRealOperators(String expression, String symbol, long expected) {
        assertEquals(expected, count(expression).getOrDefault(symbol, 0L),
                () -> expression + " 中 " + symbol + " 的次数不对");
    }

    @Test
    @DisplayName("一元正负号不算运算符")
    void shouldIgnoreUnarySigns() {
        // -5+8：只有一个加法，开头的负号是数字的一部分
        assertEquals(Map.of("+", 1L), count("-5+8"));
        // 3*-2：只有一个乘法
        assertEquals(Map.of("*", 1L), count("3*-2"));
        // abs(-7)：完全没有运算符
        assertTrue(count("abs(-7)").isEmpty(), "abs(-7) 不应统计出任何运算符");
        // (-2) 同理
        assertTrue(count("(-2)").isEmpty(), "(-2) 不应统计出任何运算符");
    }

    @Test
    @DisplayName("连着写的一减一负号只算一次减法")
    void shouldDistinguishBinaryFromUnaryInSameExpression() {
        // 1--2 是 1 减去 (-2)，只算一次减法
        assertEquals(Map.of("-", 1L), count("1--2"));
    }

    @Test
    @DisplayName("四种基本运算各一次")
    void shouldCountMixedOperators() {
        assertEquals(Map.of("+", 1L, "-", 1L, "*", 1L, "/", 1L), count("1+2-3*4/5"));
    }

    @Test
    @DisplayName("无法解析的表达式返回空表而不是抛异常")
    void shouldTolerateUnparsableExpression() {
        assertTrue(count("1#2").isEmpty());
        assertTrue(count("").isEmpty());
    }
}
