package com.fzu.calculator.calculator;

import com.fzu.calculator.exception.BusinessException;
import com.fzu.calculator.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 表达式解析器单元测试。
 *
 * <p>这些用例同时也是作业要求的"功能演示清单"：
 * 基本运算、优先级、括号、一元正负号、小数、除零、非法表达式。
 */
class ExpressionParserTest {

    private static final int DIVISION_SCALE = 10;
    private static final int MAX_NESTING_DEPTH = 50;

    private static BigDecimal eval(String expression) {
        return new ExpressionParser(expression, DIVISION_SCALE, MAX_NESTING_DEPTH).evaluate();
    }

    private static void assertResult(String expression, String expected) {
        assertEquals(0, eval(expression).compareTo(new BigDecimal(expected)),
                () -> expression + " 计算结果不符合预期");
    }

    private static BusinessException assertFails(String expression, ErrorCode expectedCode) {
        BusinessException ex = assertThrows(BusinessException.class, () -> eval(expression));
        assertEquals(expectedCode, ex.getErrorCode(), () -> expression + " 的错误码不符合预期");
        return ex;
    }

    @Nested
    @DisplayName("功能 1：基础四则运算")
    class BasicCalculation {

        @ParameterizedTest(name = "{0} = {1}")
        @CsvSource({
                "12+8,        20",
                "9-4,          5",
                "6*7,         42",
                "20/5,         4",
                "0+0,          0",
                "100-250,   -150"
        })
        void shouldCalculateBasicOperations(String expression, String expected) {
            assertResult(expression, expected);
        }
    }

    @Nested
    @DisplayName("功能 2：复合表达式")
    class CompoundExpression {

        @ParameterizedTest(name = "{0} = {1}")
        @CsvSource({
                // 运算符优先级：乘除先于加减
                "1+2*3,           7",
                "10/2+7,         12",
                "8-3*2,           2",
                "2+3*4-5,         9",
                // 括号
                "(1+2)*3,         9",
                "((2+3)*4)-5,    15",
                "2*(3+(4-1)),    12",
                // 一元正负号
                "-5+8,            3",
                "3*-2,           -6",
                "-(-5),           5",
                "-2*-3,           6",
                "+7,              7",
                "-(2+3)*2,      -10",
                // 小数
                "0.1+0.2,       0.3",
                "2*1.5,          3",
                "1.5-0.25,    1.25",
                ".5+.5,          1",
                // 空格与全角符号
                "' 1 + 2 ',       3",
                "6×7,            42",
                "8÷2,             4",
                "（1+2）×3,       9"
        })
        void shouldRespectMathRules(String expression, String expected) {
            assertResult(expression, expected);
        }

        @Test
        @DisplayName("除法保留 10 位小数并四舍五入")
        void shouldRoundDivisionResult() {
            assertResult("10/3", "3.3333333333");
            assertResult("1/8", "0.125");
            assertResult("2/3", "0.6666666667");
        }

        @Test
        @DisplayName("百分号：x% 等于 x/100")
        void shouldSupportPercentage() {
            assertResult("50%", "0.5");
            assertResult("100%", "1");
            assertResult("200*10%", "20");
            assertResult("(1+2)%", "0.03");
            assertResult("50%+25%", "0.75");
            assertResult("-50%", "-0.5");
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ErrorHandling {

        @Test
        @DisplayName("空表达式")
        void shouldRejectEmptyExpression() {
            assertFails("", ErrorCode.EXPRESSION_EMPTY);
            assertFails("    ", ErrorCode.EXPRESSION_EMPTY);
        }

        @Test
        @DisplayName("除零")
        void shouldRejectDivisionByZero() {
            assertFails("10/0", ErrorCode.DIVIDE_BY_ZERO);
            assertFails("5/(3-3)", ErrorCode.DIVIDE_BY_ZERO);
            assertFails("1/0.0", ErrorCode.DIVIDE_BY_ZERO);
        }

        @ParameterizedTest(name = "非法表达式：{0}")
        @ValueSource(strings = {
                "1+",
                "*3",
                "(1+2",
                "1+2)",
                "(1+2)3",
                "1 2",
                "1..2",
                ".",
                "()",
                "1++*2"
        })
        void shouldRejectInvalidExpression(String expression) {
            assertFails(expression, ErrorCode.INVALID_EXPRESSION);
        }

        @Test
        @DisplayName("非法字符")
        void shouldRejectUnsupportedCharacter() {
            assertFails("1+abc", ErrorCode.UNSUPPORTED_CHARACTER);
            assertFails("1^2", ErrorCode.UNSUPPORTED_CHARACTER);
            assertFails("2#3", ErrorCode.UNSUPPORTED_CHARACTER);
        }

        @Test
        @DisplayName("括号嵌套过深")
        void shouldRejectTooDeepNesting() {
            String deep = "(".repeat(60) + "1" + ")".repeat(60);
            assertFails(deep, ErrorCode.INVALID_EXPRESSION);
        }

        @Test
        @DisplayName("结果超出可存储范围")
        void shouldRejectOutOfRangeResult() {
            assertFails("99999999999999999999999999999*10", ErrorCode.NUMBER_OUT_OF_RANGE);
        }
    }

    @Nested
    @DisplayName("结果格式化")
    class Formatting {

        @Test
        @DisplayName("去掉尾随零且不使用科学计数法")
        void shouldFormatPlainString() {
            assertResult("1.50+1.50", "3");
            assertResult("200/2", "100");
            assertEquals("100", BigDecimals.toPlainString(new BigDecimal("1E+2")));
            assertEquals("3", BigDecimals.toPlainString(new BigDecimal("3.00")));
            assertEquals("0", BigDecimals.toPlainString(BigDecimal.ZERO));
        }
    }
}
