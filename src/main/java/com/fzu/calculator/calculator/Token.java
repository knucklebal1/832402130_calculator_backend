package com.fzu.calculator.calculator;

import java.math.BigDecimal;

/**
 * 词法单元。
 *
 * @param type     单元类型
 * @param lexeme   原始文本（用于错误提示）
 * @param value    数字值，非数字单元为 {@code null}
 * @param position 在原始表达式中的起始下标（从 0 开始，用于定位错误）
 */
public record Token(TokenType type, String lexeme, BigDecimal value, int position) {

    public static Token of(TokenType type, String lexeme, int position) {
        return new Token(type, lexeme, null, position);
    }

    public static Token number(String lexeme, BigDecimal value, int position) {
        return new Token(TokenType.NUMBER, lexeme, value, position);
    }
}
