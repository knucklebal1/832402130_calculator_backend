package com.fzu.calculator.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化工具：接口统一输出 {@code yyyy-MM-dd HH:mm:ss}，方便前端直接展示。
 */
public final class DateTimes {

    public static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimes() {
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DISPLAY_FORMATTER);
    }
}
