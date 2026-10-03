package com.fzu.calculator.calculator;

/**
 * 词法单元类型。
 *
 * <p>字面量统一使用半角符号（{@code + - * / ( )}），
 * 前端的 {@code × ÷} 等在词法分析阶段就会被归一化成本类型。
 */
public enum TokenType {

    /** 数字字面量，例如 12、3.14、.5 */
    NUMBER,

    /** 加号 */
    PLUS,

    /** 减号 / 负号 */
    MINUS,

    /** 乘号 */
    STAR,

    /** 除号 */
    SLASH,

    /** 左括号 */
    LPAREN,

    /** 右括号 */
    RPAREN,

    /** 表达式结束标记 */
    EOF,

    ;

    /** 是否属于算术运算符。 */
    public boolean isOperator() {
        return this == PLUS || this == MINUS || this == STAR || this == SLASH;
    }
}
