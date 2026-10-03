package com.fzu.calculator.calculator;

import com.fzu.calculator.exception.BusinessException;
import com.fzu.calculator.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 表达式解析与求值器（递归下降实现）。
 *
 * <p>文法如下，层次结构天然保证了"乘除优先于加减"：
 * <pre>
 *   expression := term (('+' | '-') term)*
 *   term       := factor (('*' | '/') factor)*
 *   factor     := ('+' | '-') factor | primary     // 处理一元正负号，如 -5、3*-2
 *   primary    := number | '(' expression ')'
 *   number     := digits ['.' digits]
 * </pre>
 *
 * <p>本类完全没有使用 {@code eval}、{@code ScriptEngine} 等"把用户输入当程序执行"的手段，
 * 而是显式地按上述文法逐层下降求值，未知结构的输入一律拒绝。
 *
 * <p>全程使用 {@link BigDecimal} 避免二进制浮点误差，
 * 除法按指定 scale 做四舍五入（HALF_UP）。
 */
public class ExpressionParser {

    private final List<Token> tokens;
    private final int divisionScale;
    private final int maxNestingDepth;

    private int index;
    private int depth;

    /**
     * @param expression      用户输入的表达式
     * @param divisionScale   除法保留的小数位数
     * @param maxNestingDepth 括号最大嵌套层数
     */
    public ExpressionParser(String expression, int divisionScale, int maxNestingDepth) {
        this.tokens = new Lexer(expression).tokenize();
        this.divisionScale = divisionScale;
        this.maxNestingDepth = maxNestingDepth;
    }

    /**
     * 解析并计算表达式的值。
     *
     * @throws BusinessException 表达式为空、语法错误、除零或结果越界
     */
    public BigDecimal evaluate() {
        if (peek().type() == TokenType.EOF) {
            throw new BusinessException(ErrorCode.EXPRESSION_EMPTY);
        }

        BigDecimal result = parseExpression();

        // 关键：必须消费完所有 Token，否则 (1+2)3 这类表达式会被误判为合法
        if (peek().type() != TokenType.EOF) {
            Token unexpected = peek();
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Unexpected token '" + unexpected.lexeme() + "' at position "
                            + (unexpected.position() + 1));
        }
        return BigDecimals.normalize(BigDecimals.checkRange(result));
    }

    /** expression := term (('+' | '-') term)* */
    private BigDecimal parseExpression() {
        BigDecimal value = parseTerm();
        while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            TokenType operator = next().type();
            BigDecimal right = parseTerm();
            value = (operator == TokenType.PLUS) ? value.add(right) : value.subtract(right);
            BigDecimals.checkRange(value);
        }
        return value;
    }

    /** term := factor (('*' | '/') factor)* */
    private BigDecimal parseTerm() {
        BigDecimal value = parseFactor();
        while (peek().type() == TokenType.STAR || peek().type() == TokenType.SLASH) {
            TokenType operator = next().type();
            BigDecimal right = parseFactor();
            value = (operator == TokenType.STAR) ? value.multiply(right) : divide(value, right);
            BigDecimals.checkRange(value);
        }
        return value;
    }

    /** factor := ('+' | '-') factor | primary */
    private BigDecimal parseFactor() {
        if (peek().type() == TokenType.PLUS) {
            next();
            return parseFactor();
        }
        if (peek().type() == TokenType.MINUS) {
            next();
            return parseFactor().negate();
        }
        return parsePrimary();
    }

    /** primary := number | '(' expression ')' */
    private BigDecimal parsePrimary() {
        Token token = peek();

        if (token.type() == TokenType.NUMBER) {
            next();
            return token.value();
        }

        if (token.type() == TokenType.LPAREN) {
            next();
            if (++depth > maxNestingDepth) {
                throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                        "Parentheses nested too deeply (max " + maxNestingDepth + ")");
            }
            BigDecimal value = parseExpression();
            if (peek().type() != TokenType.RPAREN) {
                throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                        "Missing closing parenthesis");
            }
            next();
            depth--;
            return value;
        }

        String description = token.type() == TokenType.EOF
                ? "Unexpected end of expression"
                : "Unexpected token '" + token.lexeme() + "' at position " + (token.position() + 1);
        throw new BusinessException(ErrorCode.INVALID_EXPRESSION, description);
    }

    /** 除法：除零单独报错，结果按 divisionScale 四舍五入。 */
    private BigDecimal divide(BigDecimal left, BigDecimal right) {
        if (right.signum() == 0) {
            throw new BusinessException(ErrorCode.DIVIDE_BY_ZERO);
        }
        if (left.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return left.divide(right, divisionScale, RoundingMode.HALF_UP);
    }

    private Token peek() {
        return tokens.get(index);
    }

    private Token next() {
        return tokens.get(index++);
    }
}
