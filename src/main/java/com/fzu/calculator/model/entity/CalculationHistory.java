package com.fzu.calculator.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计算历史实体，对应表 {@code calculation_history}。
 *
 * <p>只保存"计算成功"的记录，表达式错误、除零等失败请求不入库。
 */
@Entity
@Table(name = "calculation_history")
public class CalculationHistory {

    /** 主键，数据库自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 计算表达式 */
    @Column(name = "expression", nullable = false, length = 255)
    private String expression;

    /** 计算结果，最多 28 位整数 + 10 位小数 */
    @Column(name = "result", nullable = false, precision = 38, scale = 10)
    private BigDecimal result;

    /** 计算时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** JPA 规范要求保留无参构造方法。 */
    protected CalculationHistory() {
    }

    public CalculationHistory(String expression, BigDecimal result) {
        this.expression = expression;
        this.result = result;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public BigDecimal getResult() {
        return result;
    }

    public void setResult(BigDecimal result) {
        this.result = result;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
