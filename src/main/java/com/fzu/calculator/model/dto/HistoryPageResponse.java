package com.fzu.calculator.model.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 历史记录分页响应体。
 *
 * <p>示例：{@code {"success":true,"total":3,"page":1,"size":20,"pages":1,"list":[...]}}
 */
public record HistoryPageResponse(
        boolean success,
        long total,
        int page,
        int size,
        int pages,
        List<HistoryItem> list
) {

    public static HistoryPageResponse of(Page<?> pageData, List<HistoryItem> items) {
        return new HistoryPageResponse(
                true,
                pageData.getTotalElements(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalPages(),
                items
        );
    }
}
