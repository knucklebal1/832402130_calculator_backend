package com.fzu.calculator.controller;

import com.fzu.calculator.model.dto.CalculateRequest;
import com.fzu.calculator.model.dto.CalculateResponse;
import com.fzu.calculator.service.CalculateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计算接口。
 *
 * <p>{@code POST /api/calculate}，请求体 {@code {"expression":"(1+2)*3"}}。
 */
@RestController
@RequestMapping("/api")
public class CalculateController {

    private final CalculateService calculateService;

    public CalculateController(CalculateService calculateService) {
        this.calculateService = calculateService;
    }

    @PostMapping("/calculate")
    public CalculateResponse calculate(@Valid @RequestBody CalculateRequest request) {
        return calculateService.calculate(request.expression(), request.angleInDegrees());
    }
}
