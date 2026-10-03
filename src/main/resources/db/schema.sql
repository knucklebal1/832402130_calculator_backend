-- =====================================================================
--  前后端分离计算器系统 —— 数据库初始化脚本
--  用法：mysql -u root -p < schema.sql
--  说明：应用默认 ddl-auto=update 也会自动建表，本脚本用于手动初始化
--        以及 Docker 容器首次启动时挂载到 /docker-entrypoint-initdb.d/
-- =====================================================================

-- 声明本脚本内容为 utf8mb4，避免 Windows 控制台默认 GBK 导致中文注释乱码
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS calculator_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE calculator_db;

DROP TABLE IF EXISTS calculation_history;

CREATE TABLE calculation_history
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    expression VARCHAR(255)    NOT NULL COMMENT '用户输入的计算表达式，例如 (1+2)*3',
    result     DECIMAL(38, 10) NOT NULL COMMENT '后端计算得到的结果，最多 28 位整数 + 10 位小数',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '计算时间（毫秒精度）',
    PRIMARY KEY (id),
    KEY idx_history_created_at (created_at DESC),
    KEY idx_history_expression (expression(64))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='计算历史表';
