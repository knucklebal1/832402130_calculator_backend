package com.fzu.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 前后端分离计算器系统 —— 后端启动类。
 *
 * <p>本服务只负责：接收表达式、校验、解析、计算、持久化历史记录、返回结果。
 * 前端不参与任何核心计算。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalculatorApplication.class, args);
    }
}
