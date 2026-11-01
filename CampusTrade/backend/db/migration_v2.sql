-- ========================================
-- 校园二手交易平台 - v2 升级脚本
-- 新增：交易评价、退款流程、系统通知、字段补充
-- 执行方式: mysql -uroot -p123456 campus_trade < migration_v2.sql
-- ========================================

USE campus_trade;

-- 1. 订单表：评价标记、退款状态
ALTER TABLE `orders`
    ADD COLUMN `is_rated` TINYINT NOT NULL DEFAULT 0 COMMENT '买家是否已评价: 0-否 1-是' AFTER `remark`,
    ADD COLUMN `seller_rated` TINYINT NOT NULL DEFAULT 0 COMMENT '卖家是否已评价: 0-否 1-是' AFTER `is_rated`,
    ADD COLUMN `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态: 0-无 1-申请中 2-已退款 3-已拒绝' AFTER `seller_rated`,
    ADD COLUMN `refund_reason` VARCHAR(500) DEFAULT NULL COMMENT '退款原因' AFTER `refund_status`;

-- 2. 交易评价表
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `rater_id` BIGINT NOT NULL COMMENT '评价人ID',
    `ratee_id` BIGINT NOT NULL COMMENT '被评价人ID',
    `score` TINYINT NOT NULL COMMENT '评分 1-5',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_rater` (`order_id`, `rater_id`),
    KEY `idx_ratee_id` (`ratee_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易评价表';

-- 3. 系统通知表
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收人ID',
    `type` VARCHAR(30) NOT NULL COMMENT '通知类型: ORDER_PAID/ORDER_SHIPPED/REFUND_APPLY...',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '内容',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联ID(订单/商品等)',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';