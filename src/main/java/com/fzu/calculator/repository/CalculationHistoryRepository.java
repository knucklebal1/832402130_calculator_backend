package com.fzu.calculator.repository;

import com.fzu.calculator.model.entity.CalculationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 把自增主键重置回 1。
     *
     * <p>MySQL 的自增值在删掉数据后不会自动回退：如果不清零，
     * 清空历史后再算第一题会得到 #169 这样的编号。
     *
     * <p>单独开一个事务执行 DDL，避免和删除操作混在同一个事务里
     * （MySQL 的 DDL 会隐式提交，混在一起容易让事务边界变得不清晰）。
     */
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE calculation_history AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
}
