package com.fzu.calculator.calculator;

import com.fzu.calculator.exception.BusinessException;
import com.fzu.calculator.exception.ErrorCode;

import java.math.BigDecimal;

/**
 * BigDecimal 工具方法：范围校验与结果格式化。
 *
 * <p>数据库中 result 字段为 {@code DECIMAL(38,10)}，整数部分最多 28 位，
 * 所以这里把上界设为 {@code 1E28}，超出即视为越界，避免写入数据库时报错。
 */
public final class BigDecimals {

    /** 允许的绝对值上界（不含），与 DECIMAL(38,10) 的整数位容量对应。 */
    public static final BigDecimal MAX_MAGNITUDE = new BigDecimal("1E28");

    private BigDecimals() {
    }

    /**
     * 去掉无意义的尾随零，并保证 scale 不小于 0。
     *
     * <p>{@code stripTrailingZeros()} 会把 100 变成 1E+2，
     * 直接序列化成 JSON 会输出科学计数法，因此这里把 scale 还原到 0。
     */
    public static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    /** 校验数值是否在可存储范围内，越界抛出 {@link ErrorCode#NUMBER_OUT_OF_RANGE}。 */
    public static BigDecimal checkRange(BigDecimal value) {
        if (value.abs().compareTo(MAX_MAGNITUDE) >= 0) {
            throw new BusinessException(ErrorCode.NUMBER_OUT_OF_RANGE,
                    "Result is out of range (|value| must be < 1E28)");
        }
        return value;
    }

    /** 把结果转成不带科学计数法的字符串，供接口返回给前端展示。 */
    public static String toPlainString(BigDecimal value) {
        return normalize(value).toPlainString();
    }
}
