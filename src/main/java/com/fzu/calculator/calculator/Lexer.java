package com.fzu.calculator.calculator;

import com.fzu.calculator.exception.BusinessException;
import com.fzu.calculator.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 词法分析器：把表达式字符串切分成 {@link Token} 序列。
 *
 * <p>只识别数字、四则运算符和括号。任何非法字符都会立即抛出业务异常，
 * 因此后续的语法分析阶段可以假定输入中的字符都是"干净"的。
 */
public class Lexer {

    private final String source;
    private int position;

    public Lexer(String source) {
        this.source = source;
    }

    /**
     * 执行词法分析。
     *
     * @return Token 列表，最后一项一定是 {@link TokenType#EOF}
     * @throws BusinessException 表达式含非法字符或数字格式错误
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (position < source.length()) {
            char current = source.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }
            if (isDigit(current) || current == '.') {
                tokens.add(readNumber());
                continue;
            }
            if (isLetter(current)) {
                tokens.add(readIdentifier());
                continue;
            }
            // 允许直接输入圆周率符号 π
            if (current == '\u03C0') {
                tokens.add(Token.of(TokenType.IDENTIFIER, "pi", position));
                position++;
                continue;
            }

            TokenType type = switch (current) {
                case '+' -> TokenType.PLUS;
                case '-', '\u2212', '\u2013' -> TokenType.MINUS;  // - 、−(U+2212)、–(U+2013)
                case '*', '\u00D7', '\u00B7' -> TokenType.STAR;   // * 、×(U+00D7)、·(U+00B7)
                case '/', '\u00F7' -> TokenType.SLASH;            // / 、÷(U+00F7)
                case '%', '\uFF05' -> TokenType.PERCENT;          // % 、％
                case '^' -> TokenType.CARET;
                case '!' -> TokenType.BANG;
                case '(', '\uFF08' -> TokenType.LPAREN;           // ( 、（
                case ')', '\uFF09' -> TokenType.RPAREN;           // ) 、）
                default -> null;
            };

            if (type == null) {
                throw new BusinessException(ErrorCode.UNSUPPORTED_CHARACTER,
                        "Unsupported character '" + current + "' at position " + (position + 1));
            }
            tokens.add(Token.of(type, String.valueOf(current), position));
            position++;
        }
        tokens.add(Token.of(TokenType.EOF, "", position));
        return tokens;
    }

    /** 读取一个数字字面量。 */
    private Token readNumber() {
        int start = position;
        boolean dotSeen = false;

        while (position < source.length()) {
            char current = source.charAt(position);
            if (isDigit(current)) {
                position++;
            } else if (current == '.' && !dotSeen) {
                dotSeen = true;
                position++;
            } else {
                break;
            }
        }

        String lexeme = source.substring(start, position);
        if (".".equals(lexeme)) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Misplaced decimal point at position " + (start + 1));
        }
        try {
            // 用字符串构造 BigDecimal，避免 double 的精度损失
            return Token.number(lexeme, new BigDecimal(lexeme), start);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Malformed number '" + lexeme + "' at position " + (start + 1));
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** 读取标识符（常量名或函数名），统一转成小写，便于后续匹配。 */
    private Token readIdentifier() {
        int start = position;
        while (position < source.length() && isLetter(source.charAt(position))) {
            position++;
        }
        String lexeme = source.substring(start, position);
        return Token.of(TokenType.IDENTIFIER, lexeme.toLowerCase(Locale.ROOT), start);
    }
}
