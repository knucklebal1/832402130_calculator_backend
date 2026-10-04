package com.fzu.calculator.calculator;

import com.fzu.calculator.exception.BusinessException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运算符使用统计。
 *
 * <p>统计的是"真正做了一次运算"的运算符：
 * 二元运算符 {@code + - * / ^} 与后缀运算符 {@code % !}。
 *
 * <p>一元正负号只是数字的一部分，不算运算符。例如：
 * <ul>
 *   <li>{@code -5+8} 里只有 1 个加法，前面的负号不计</li>
 *   <li>{@code 3*-2} 里只有 1 个乘法，负号不计</li>
 *   <li>{@code abs(-7)} 里没有运算符</li>
 * </ul>
 *
 * <p>判断依据是记号所处的位置：若上一个记号后面本该出现一个操作数
 * （表达式开头、左括号后、其它运算符后），那么这里的 {@code +} / {@code -}
 * 就是正负号；否则才是加减法。
 */
public final class OperatorAnalyzer {

    private OperatorAnalyzer() {
    }

    /**
     * 统计一个表达式中各运算符出现的次数。
     *
     * @param expression 已经计算成功并入库的表达式
     * @return 运算符符号到次数的映射；表达式无法解析时返回空表，不影响其它记录
     */
    public static Map<String, Long> countUsages(String expression) {
        Map<String, Long> counts = new LinkedHashMap<>();

        List<Token> tokens;
        try {
            tokens = new Lexer(expression).tokenize();
        } catch (BusinessException ex) {
            // 能入库的表达式理论上都是合法的；真遇到解析不了的旧数据就跳过
            return counts;
        }

        TokenType previous = null;
        for (Token token : tokens) {
            TokenType type = token.type();
            if (isRealOperation(type, previous)) {
                counts.merge(symbolOf(type), 1L, Long::sum);
            }
            previous = type;
        }
        return counts;
    }

    /** 该记号是否为一次真正的运算（而不是正负号）。 */
    private static boolean isRealOperation(TokenType type, TokenType previous) {
        return switch (type) {
            case STAR, SLASH, CARET, PERCENT, BANG -> true;
            // +/- 处于"该写操作数"的位置时是正负号，不计入统计
            case PLUS, MINUS -> !expectsOperand(previous);
            default -> false;
        };
    }

    /** 上一个记号之后是否应该出现一个操作数。 */
    private static boolean expectsOperand(TokenType previous) {
        return previous == null
                || previous == TokenType.LPAREN
                || previous == TokenType.PLUS
                || previous == TokenType.MINUS
                || previous == TokenType.STAR
                || previous == TokenType.SLASH
                || previous == TokenType.CARET;
    }

    /** 运算符在界面上展示用的符号。 */
    private static String symbolOf(TokenType type) {
        return switch (type) {
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case CARET -> "^";
            case PERCENT -> "%";
            case BANG -> "!";
            default -> type.name();
        };
    }
}
