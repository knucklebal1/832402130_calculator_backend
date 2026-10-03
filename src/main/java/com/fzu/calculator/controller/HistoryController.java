package com.fzu.calculator.controller;

import com.fzu.calculator.model.dto.DeleteAllResponse;
import com.fzu.calculator.model.dto.HistoryPageResponse;
import com.fzu.calculator.model.dto.HistoryStatsResponse;
import com.fzu.calculator.service.HistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计算历史接口。
 *
 * <ul>
 *   <li>{@code GET    /api/history?page=1&size=20&keyword=1%2B2} 分页查询</li>
 *   <li>{@code DELETE /api/history/{id}} 删除单条，成功返回 204</li>
 *   <li>{@code DELETE /api/history} 清空全部（加分项）</li>
 *   <li>{@code GET    /api/history/stats} 统计信息（加分项）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public HistoryPageResponse list(@RequestParam(defaultValue = "") String keyword,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return historyService.query(keyword, page, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        historyService.deleteById(id);
    }

    @DeleteMapping
    public DeleteAllResponse deleteAll() {
        return new DeleteAllResponse(true, historyService.deleteAll());
    }

    @GetMapping("/stats")
    public HistoryStatsResponse stats() {
        return historyService.stats();
    }
}
