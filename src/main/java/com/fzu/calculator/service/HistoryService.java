package com.fzu.calculator.service;

import com.fzu.calculator.calculator.BigDecimals;
import com.fzu.calculator.calculator.OperatorAnalyzer;
import com.fzu.calculator.exception.BusinessException;
import com.fzu.calculator.exception.ErrorCode;
import com.fzu.calculator.model.dto.HistoryItem;
import com.fzu.calculator.model.dto.HistoryPageResponse;
import com.fzu.calculator.model.dto.HistoryStatsResponse;
import com.fzu.calculator.model.entity.CalculationHistory;
import com.fzu.calculator.repository.CalculationHistoryRepository;
import com.fzu.calculator.util.DateTimes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算历史服务：分页查询、关键字搜索、删除单条、清空、统计。
 *
 * <p>历史数据全部来自数据库，不依赖任何前端缓存或应用内存。
 */
@Service
public class HistoryService {

    /** 每页最大条数，防止前端传入过大的 size 拖垮数据库。 */
    private static final int MAX_PAGE_SIZE = 100;

    private final CalculationHistoryRepository historyRepository;

    public HistoryService(CalculationHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * 分页查询历史记录，支持按表达式关键字模糊搜索。
     *
     * @param keyword 关键字，可为空
     * @param page    页码，从 1 开始（小于 1 会被纠正为 1）
     * @param size    每页条数（会被限制在 1 ~ 100）
     */
    @Transactional(readOnly = true)
    public HistoryPageResponse query(String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        Page<CalculationHistory> result = (keyword == null || keyword.isBlank())
                ? historyRepository.findAllByOrderByIdDesc(pageable)
                : historyRepository.searchByExpression(escapeLike(keyword.trim()), pageable);

        List<HistoryItem> items = result.getContent().stream().map(HistoryItem::from).toList();
        return HistoryPageResponse.of(result, items);
    }

    /**
     * 删除指定 id 的历史记录。
     *
     * @throws BusinessException 记录不存在时抛出 404
     */
    @Transactional
    public void deleteById(Long id) {
        if (!historyRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.HISTORY_NOT_FOUND,
                    "History record " + id + " not found");
        }
        historyRepository.deleteById(id);
    }

    /** 清空全部历史记录，返回被删除的条数。 */
    @Transactional
    public long deleteAll() {
        long count = historyRepository.count();
        if (count > 0) {
            historyRepository.deleteAllInBatch();
        }
        return count;
    }

    /** 计算统计信息（加分项）：总条数、最常用运算符、平均结果、最近一次计算时间。 */
    @Transactional(readOnly = true)
    public HistoryStatsResponse stats() {
        List<CalculationHistory> all = historyRepository.findAll();
        if (all.isEmpty()) {
            return new HistoryStatsResponse(true, 0, null, 0, null, null);
        }

        // 逐条解析历史记录里的表达式，只统计真正做了运算的运算符。
        // 不能直接数 + - * / 字符：abs(-7) 里的负号、3*-2 里的负号都不是运算符，
        // 而且 ^ ! % 这些运算符也要一并统计进来。
        Map<String, Long> operatorCounts = new LinkedHashMap<>();
        for (CalculationHistory history : all) {
            OperatorAnalyzer.countUsages(history.getExpression())
                    .forEach((symbol, count) -> operatorCounts.merge(symbol, count, Long::sum));
        }

        Map.Entry<String, Long> top = operatorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        BigDecimal sum = all.stream()
                .map(CalculationHistory::getResult)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(all.size()), 6, RoundingMode.HALF_UP);

        String latestAt = all.stream()
                .map(CalculationHistory::getCreatedAt)
                .max(Comparator.naturalOrder())
                .map(DateTimes::format)
                .orElse(null);

        return new HistoryStatsResponse(
                true,
                all.size(),
                top == null ? null : top.getKey(),
                top == null ? 0L : top.getValue(),
                BigDecimals.toPlainString(average),
                latestAt
        );
    }

    /**
     * 转义 LIKE 模式中的特殊字符，配合 SQL 里的 {@code ESCAPE '!'} 使用。
     *
     * <p>否则用户搜索 "%" 会匹配到全部记录。
     */
    private static String escapeLike(String keyword) {
        return keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
