package com.fzu.calculator.service;

import com.fzu.calculator.calculator.ExpressionParser;
import com.fzu.calculator.config.CalculatorProperties;
import com.fzu.calculator.exception.BusinessException;
import com.fzu.calculator.exception.ErrorCode;
import com.fzu.calculator.model.dto.CalculateResponse;
import com.fzu.calculator.model.entity.CalculationHistory;
import com.fzu.calculator.repository.CalculationHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 计算服务：校验表达式 → 解析求值 → 保存历史 → 返回结果。
 *
 * <p>这是整个系统的核心。前端只负责把表达式发过来，最终结果完全由这里产生。
 */
@Service
public class CalculateService {

    private final CalculationHistoryRepository historyRepository;
    private final CalculatorProperties properties;

    public CalculateService(CalculationHistoryRepository historyRepository,
                            CalculatorProperties properties) {
        this.historyRepository = historyRepository;
        this.properties = properties;
    }

    /**
     * 计算表达式并记录历史。
     *
     * <p>使用 {@code @Transactional} 保证"计算成功"与"写入历史"在同一事务中：
     * 入库失败时整个请求回滚，不会出现"算出来了但历史没记住"的中间状态。
     */
    @Transactional
    public CalculateResponse calculate(String rawExpression) {
        String expression = rawExpression == null ? "" : rawExpression.trim();

        if (expression.isEmpty()) {
            throw new BusinessException(ErrorCode.EXPRESSION_EMPTY);
        }
        if (expression.length() > properties.maxExpressionLength()) {
            throw new BusinessException(ErrorCode.EXPRESSION_TOO_LONG,
                    "Expression is too long (max " + properties.maxExpressionLength() + " characters)");
        }

        BigDecimal result = new ExpressionParser(
                expression,
                properties.divisionScale(),
                properties.maxNestingDepth()
        ).evaluate();

        CalculationHistory saved = historyRepository.save(new CalculationHistory(expression, result));
        return CalculateResponse.from(saved);
    }
}
