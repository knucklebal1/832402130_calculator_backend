package com.fzu.calculator.repository;

import com.fzu.calculator.model.entity.CalculationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalculationHistoryRepository extends JpaRepository<CalculationHistory, Long> {

    /** 按 id 倒序分页查询（新的在前）。 */
    Page<CalculationHistory> findAllByOrderByIdDesc(Pageable pageable);

    /**
     * 按表达式关键字模糊搜索。
     *
     * <p>使用 native SQL 并显式声明 {@code ESCAPE '!'}，
     * 配合 Service 层对 {@code % _ !} 的转义，避免用户输入的 {@code %}
     * 被当成通配符导致"搜什么都能匹配"。
     */
    @Query(
            value = """
                    SELECT * FROM calculation_history
                    WHERE expression LIKE CONCAT('%', :keyword, '%') ESCAPE '!'
                    ORDER BY id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM calculation_history
                    WHERE expression LIKE CONCAT('%', :keyword, '%') ESCAPE '!'
                    """,
            nativeQuery = true
    )
    Page<CalculationHistory> searchByExpression(@Param("keyword") String keyword, Pageable pageable);
}
