-- ========================================
-- v4 迁移脚本：钱包与担保交易
-- 执行：mysql -u root -p campus_trade < backend/db/migration_v4_wallet.sql
-- ========================================

-- ----------------------------------------
-- 用户钱包：可用余额 + 冻结余额
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `wallet` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `available_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    `frozen_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额（担保交易在途资金）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户钱包表';

-- ----------------------------------------
-- 资金流水
-- ----------------------------------------
CREATE TABLE IF NOT EXISTS `trade_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `trade_no` VARCHAR(64) NOT NULL COMMENT '流水号',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID，充值为空',
    `user_id` BIGINT NOT NULL COMMENT '资金所属用户',
    `type` TINYINT NOT NULL COMMENT '类型: 1-充值 2-支付冻结 3-退款解冻 4-结算转出 5-结算入账',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '发生金额（正数，方向由 type 决定）',
    `available_after` DECIMAL(12,2) NOT NULL COMMENT '交易后可用余额',
    `frozen_after` DECIMAL(12,2) NOT NULL COMMENT '交易后冻结余额',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_no` (`trade_no`),
    -- 支付/退款/结算回调可能重复到达，数据库层兜底保证同一订单同一类型只记一次
    UNIQUE KEY `uk_order_type_user` (`order_id`, `type`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表';