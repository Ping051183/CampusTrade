-- ========================================
-- 校园二手交易平台 - 数据库初始化脚本
-- 数据库: campus_trade
-- 版本: 1.0.0
-- ========================================

CREATE DATABASE IF NOT EXISTS campus_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_trade;

-- ----------------------------------------
-- 用户表
-- ----------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `student_no` VARCHAR(50) DEFAULT NULL COMMENT '学号',
    `school` VARCHAR(100) DEFAULT NULL COMMENT '学校',
    `role` TINYINT NOT NULL DEFAULT 1 COMMENT '角色: 0-管理员 1-普通用户',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_school` (`school`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------------------
-- 商品分类表
-- ----------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ----------------------------------------
-- 商品表
-- ----------------------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `title` VARCHAR(200) NOT NULL COMMENT '商品标题',
    `description` TEXT COMMENT '商品描述',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `images` VARCHAR(2000) DEFAULT NULL COMMENT '图片URL(多个用逗号分隔)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核 1-在售 2-已下架 3-已售出 4-审核驳回',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    FULLTEXT KEY `ft_title_desc` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ----------------------------------------
-- 收藏表
-- ----------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ----------------------------------------
-- 订单表
-- ----------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
    `buyer_id` BIGINT NOT NULL COMMENT '买家ID',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待付款 1-已付款 2-已发货 3-已收货 4-已取消 5-已完成',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `is_rated` TINYINT NOT NULL DEFAULT 0 COMMENT '买家是否已评价: 0-否 1-是',
    `seller_rated` TINYINT NOT NULL DEFAULT 0 COMMENT '卖家是否已评价: 0-否 1-是',
    `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态: 0-无 1-申请中 2-已退款 3-已拒绝',
    `refund_reason` VARCHAR(500) DEFAULT NULL COMMENT '退款原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------------------
-- 评论表
-- ----------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ----------------------------------------
-- 举报表
-- ----------------------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
    `reason` VARCHAR(500) NOT NULL COMMENT '举报原因',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态: 0-待处理 1-已处理 2-驳回',
    `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- ----------------------------------------
-- 公告表
-- ----------------------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ----------------------------------------
-- 交易评价表
-- ----------------------------------------
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

-- ----------------------------------------
-- 系统通知表
-- ----------------------------------------
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

-- ----------------------------------------
-- 聊天消息表
-- ----------------------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `goods_id` BIGINT DEFAULT NULL COMMENT '关联商品ID',
    `content` TEXT COMMENT '消息内容',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_conversation` (`sender_id`, `receiver_id`, `goods_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入商品分类
INSERT INTO `category` (`category_name`, `sort`) VALUES
('数码产品', 1),
('图书教材', 2),
('生活用品', 3),
('体育用品', 4),
('宿舍用品', 5),
('服装鞋帽', 6),
('其他', 7);

-- 插入管理员账户 (密码: admin123, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$GuRwu7PRfefHUWAvVFiGCehjMvW.udODd/nHx9cl9YVX15Z9oDagW', '系统管理员', 0, 1);

-- ========================================
-- 索引与外键说明（通过程序保证数据一致性，不建物理外键以提升性能）
-- user.id -> goods.seller_id
-- user.id -> favorite.user_id
-- user.id -> orders.buyer_id
-- user.id -> orders.seller_id
-- user.id -> comment.user_id
-- user.id -> report.reporter_id
-- category.id -> goods.category_id
-- goods.id -> favorite.goods_id
-- goods.id -> orders.goods_id
-- goods.id -> comment.goods_id
-- goods.id -> report.goods_id
-- ========================================
