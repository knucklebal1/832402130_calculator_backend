package com.fzu.calculator.calculator;

import com.fzu.calculator.exception.BusinessException;
import com.fzu.calculator.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表达式解析与求值器（递归下降实现）。
 *
 * <p>文法如下，层次结构天然保证了"乘除优先于加减"：
 * <pre>
 *   expression := term (('+' | '-') term)*
 *   term       := unary (('*' | '/') unary)*
 *   unary      := ('+' | '-') unary | power       // 一元正负号，如 -5、3*-2
 *   power      := postfix ('^' unary)?            // 乘方，右结合：2^3^2 = 2^9 = 512
 *   postfix    := primary ('%' | '!')*            // 百分号 50%=0.5；阶乘 5!=120
 *   primary    := number | constant | function '(' expression ')' | '(' expression ')'
 *   constant   := 'pi' | 'e'
 *   function   := 'sqrt' | 'abs' | 'ln' | 'log' | 'sin' | 'cos' | 'tan'
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

    /** 数学常量。 */
    private static final Map<String, BigDecimal> CONSTANTS = Map.of(
            "pi", new BigDecimal("3.14159265358979323846"),
            "e", new BigDecimal("2.71828182845904523536")
    );

    /** 支持的函数名（小写）。 */
    private static final Set<String> FUNCTIONS = Set.of(
            "sqrt", "abs", "ln", "log", "sin", "cos", "tan"
    );

    /** 开平方时保留的内部精度，最终结果再统一按 divisionScale 四舍五入。 */
    private static final MathContext SQRT_CONTEXT = new MathContext(20);

    /** 整数指数允许的最大绝对值，防止 9^999999 这类表达式耗费大量 CPU。 */
    private static final int MAX_INTEGER_EXPONENT = 1000;

    private final List<Token> tokens;
    private final int divisionScale;
    private final int maxNestingDepth;
    private final boolean angleInDegrees;

    private int index;
    private int depth;

    /**
     * @param expression      用户输入的表达式
     * @param divisionScale   除法保留的小数位数
     * @param maxNestingDepth 括号最大嵌套层数
     */
    public ExpressionParser(String expression, int divisionScale, int maxNestingDepth) {
        this(expression, divisionScale, maxNestingDepth, true);
    }

    /**
     * @param expression      用户输入的表达式
     * @param divisionScale   除法保留的小数位数
     * @param maxNestingDepth 括号最大嵌套层数
     * @param angleInDegrees  三角函数按角度制（true）还是弧度制（false）计算
     */
    public ExpressionParser(String expression, int divisionScale, int maxNestingDepth,
                            boolean angleInDegrees) {
        this.tokens = new Lexer(expression).tokenize();
        this.divisionScale = divisionScale;
        this.maxNestingDepth = maxNestingDepth;
        this.angleInDegrees = angleInDegrees;
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
        // 统一按 divisionScale 四舍五入后再返回，保证接口返回值与数据库
        // DECIMAL(38,10) 里实际存下的值完全一致（否则数据库会二次舍入，
        // 出现"结果 1.4142135624、历史里却是别的数"这种对不上的情况）
        BigDecimal rounded = result.setScale(divisionScale, RoundingMode.HALF_UP);
        return BigDecimals.normalize(BigDecimals.checkRange(rounded));
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

    /** term := unary (('*' | '/') unary)* */
    private BigDecimal parseTerm() {
        BigDecimal value = parseUnary();
        while (peek().type() == TokenType.STAR || peek().type() == TokenType.SLASH) {
            TokenType operator = next().type();
            BigDecimal right = parseUnary();
            value = (operator == TokenType.STAR) ? value.multiply(right) : divide(value, right);
            BigDecimals.checkRange(value);
        }
        return value;
    }

    /** unary := ('+' | '-') unary | power */
    private BigDecimal parseUnary() {
        if (peek().type() == TokenType.PLUS) {
            next();
            return parseUnary();
        }
        if (peek().type() == TokenType.MINUS) {
            next();
            return parseUnary().negate();
        }
        return parsePower();
    }

    /**
     * power := postfix ('^' unary)?
     *
     * <p>乘方右结合（2^3^2 = 2^9），且优先级高于一元负号，
     * 因此 -2^2 = -(2^2) = -4，而 (-2)^2 = 4；指数位置允许再写一元负号，如 2^-1。
     */
    private BigDecimal parsePower() {
        BigDecimal base = parsePostfix();
        if (peek().type() == TokenType.CARET) {
            next();
            return power(base, parseUnary());
        }
        return base;
    }

    /**
     * postfix := primary ('%' | '!')*
     *
     * <p>百分号 x% 等于 x 除以 100，用小数点移位实现，结果精确；
     * 阶乘 n! 只接受非负整数。
     */
    private BigDecimal parsePostfix() {
        BigDecimal value = parsePrimary();
        while (true) {
            if (peek().type() == TokenType.PERCENT) {
                next();
                value = value.movePointLeft(2);
            } else if (peek().type() == TokenType.BANG) {
                next();
                value = factorial(value);
            } else {
                break;
            }
            BigDecimals.checkRange(value);
        }
        return value;
    }

    /** primary := number | constant | function '(' expression ')' | '(' expression ')' */
    private BigDecimal parsePrimary() {
        Token token = peek();

        if (token.type() == TokenType.NUMBER) {
            next();
            return token.value();
        }

        if (token.type() == TokenType.IDENTIFIER) {
            next();
            return parseIdentifier(token);
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

    /** 处理常量名与函数名。 */
    private BigDecimal parseIdentifier(Token token) {
        String name = token.lexeme();

        BigDecimal constant = CONSTANTS.get(name);
        if (constant != null) {
            return constant;
        }

        if (!FUNCTIONS.contains(name)) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Unknown name '" + name + "' at position " + (token.position() + 1));
        }

        if (peek().type() != TokenType.LPAREN) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Function '" + name + "' must be followed by parentheses");
        }
        next();
        if (++depth > maxNestingDepth) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Parentheses nested too deeply (max " + maxNestingDepth + ")");
        }

        int argumentStart = index;
        BigDecimal argument = parseExpression();

        if (peek().type() == TokenType.RPAREN) {
            next();
        } else if (isSingleOperand(argumentStart)) {
            // 宽容处理：括号里只有一个操作数（如 sqrt(2、sin(30、ln(e)时自动补上右括号
        } else {
            // 括号里是复合表达式却漏写右括号时必须报错，
            // 否则用户会把 sin(30+1 误当成 sin(30)+1，得到一个悄悄算错的结果
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Missing closing parenthesis for '" + name + "'");
        }
        depth--;
        return applyFunction(name, argument);
    }

    /**
     * 判断刚解析完的一段 Token 是否只是"一个操作数"。
     *
     * <p>操作数指一个数字字面量或一个常量名（pi、e），允许前面带一元正负号
     * （如 {@code -2}）。除此之外出现任何运算符、括号或第二个操作数都算复合表达式。
     *
     * @param startIndex 这段 Token 的起始下标（含）
     */
    private boolean isSingleOperand(int startIndex) {
        int operandCount = 0;
        for (int i = startIndex; i < index; i++) {
            TokenType type = tokens.get(i).type();
            if (type == TokenType.NUMBER || type == TokenType.IDENTIFIER) {
                operandCount++;
            } else if (type != TokenType.PLUS && type != TokenType.MINUS) {
                return false;
            }
        }
        return operandCount == 1;
    }

    /**
     * 乘方运算。
     *
     * <p>整数指数走 {@link BigDecimal#pow(int)}，结果是精确值；
     * 非整数指数（如 2^0.5）必须借助 {@link Math#pow}，此时会引入浮点误差，
     * 由最终统一的四舍五入收敛到 10 位小数。
     */
    private BigDecimal power(BigDecimal base, BigDecimal exponent) {
        BigDecimal stripped = exponent.stripTrailingZeros();

        if (stripped.scale() <= 0) {
            int exp;
            try {
                exp = stripped.intValueExact();
            } catch (ArithmeticException ex) {
                throw new BusinessException(ErrorCode.NUMBER_OUT_OF_RANGE, "Exponent is too large");
            }
            if (Math.abs((long) exp) > MAX_INTEGER_EXPONENT) {
                throw new BusinessException(ErrorCode.NUMBER_OUT_OF_RANGE,
                        "Exponent is too large (max " + MAX_INTEGER_EXPONENT + ")");
            }
            if (exp >= 0) {
                return base.pow(exp);
            }
            BigDecimal denominator = base.pow(-exp);
            if (denominator.signum() == 0) {
                throw new BusinessException(ErrorCode.DIVIDE_BY_ZERO);
            }
            return BigDecimal.ONE.divide(denominator, divisionScale + 10, RoundingMode.HALF_UP);
        }

        if (base.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "A negative base cannot be raised to a fractional exponent");
        }
        double result = Math.pow(base.doubleValue(), exponent.doubleValue());
        if (!Double.isFinite(result)) {
            throw new BusinessException(ErrorCode.NUMBER_OUT_OF_RANGE, "Result is out of range");
        }
        return BigDecimal.valueOf(result);
    }

    /**
     * 阶乘：只接受非负整数。
     *
     * <p>一边乘一边做范围校验，像 100000! 这种会在超出范围时立刻报错，
     * 不会真的把整个大数算出来。
     */
    private BigDecimal factorial(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.signum() < 0 || stripped.scale() > 0) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Factorial requires a non-negative integer");
        }

        int n;
        try {
            n = stripped.intValueExact();
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.NUMBER_OUT_OF_RANGE,
                    "Factorial operand is too large");
        }

        BigDecimal result = BigDecimal.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigDecimal.valueOf(i));
            BigDecimals.checkRange(result);
        }
        return result;
    }

    /**
     * 应用函数。
     *
     * <p>abs 用 BigDecimal 精确求值，sqrt 用 BigDecimal.sqrt 保证高精度；
     * 三角函数与对数属于超越函数，BigDecimal 没有内建实现，
     * 这里借助 double 计算后由最终的四舍五入收敛到 10 位小数。
     */
    private BigDecimal applyFunction(String name, BigDecimal argument) {
        if ("abs".equals(name)) {
            return argument.abs();
        }
        if ("sqrt".equals(name)) {
            if (argument.signum() < 0) {
                throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                        "Cannot take the square root of a negative number");
            }
            return argument.sqrt(SQRT_CONTEXT);
        }

        double x = argument.doubleValue();
        double y = switch (name) {
            case "ln" -> {
                requirePositive(x, "ln");
                yield Math.log(x);
            }
            case "log" -> {
                requirePositive(x, "log");
                yield Math.log10(x);
            }
            case "sin" -> Math.sin(toRadians(x));
            case "cos" -> Math.cos(toRadians(x));
            case "tan" -> Math.tan(toRadians(x));
            default -> throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Unknown function '" + name + "'");
        };

        if (!Double.isFinite(y)) {
            throw new BusinessException(ErrorCode.NUMBER_OUT_OF_RANGE, "Result is out of range");
        }
        return BigDecimal.valueOf(y);
    }

    private void requirePositive(double x, String name) {
        if (x <= 0) {
            throw new BusinessException(ErrorCode.INVALID_EXPRESSION,
                    "Function '" + name + "' requires a positive argument");
        }
    }

    /** 角度制时把参数换算成弧度，弧度制时原样返回。 */
    private double toRadians(double x) {
        return angleInDegrees ? Math.toRadians(x) : x;
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
