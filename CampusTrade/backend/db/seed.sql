-- ========================================
-- 演示种子数据（可选）：可直接登录、下单、演示钱包与对账
-- 执行顺序：mysql -u root -p campus_trade < backend/db/init.sql
--           mysql -u root -p campus_trade < backend/db/seed.sql
-- 所有账号密码统一为 123456，管理员 admin 密码为 admin123
-- ========================================

USE campus_trade;

-- ----------------------------------------
-- 用户（密码均为 BCrypt("123456")，管理员沿用 init.sql 的 admin123）
-- ----------------------------------------
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `student_no`, `school`, `role`, `status`) VALUES
(1, 'admin', '$2a$10$GuRwu7PRfefHUWAvVFiGCehjMvW.udODd/nHx9cl9YVX15Z9oDagW', '系统管理员', NULL, '广州工商学院', 0, 1),
(2, 'alice', '$2a$10$WxTliZ5EhKA8O5eVIkKwl.3B/NoX20Ezj675FzxgTFYnfms/T6g.u', '爱丽丝', '2021001001', '广州工商学院', 1, 1),
(3, 'bob',   '$2a$10$WxTliZ5EhKA8O5eVIkKwl.3B/NoX20Ezj675FzxgTFYnfms/T6g.u', '鲍勃',   '2021001002', '广州工商学院', 1, 1),
(4, 'carol', '$2a$10$WxTliZ5EhKA8O5eVIkKwl.3B/NoX20Ezj675FzxgTFYnfms/T6g.u', '卡罗尔', '2021001003', '广州工商学院', 1, 1);

-- ----------------------------------------
-- 钱包 + 对应充值流水（流水净额与钱包余额一致，对账接口应返回空列表）
-- ----------------------------------------
INSERT IGNORE INTO `wallet` (`user_id`, `available_amount`, `frozen_amount`) VALUES
(2, 1000.00, 0.00),
(3, 500.00, 0.00),
(4, 0.00, 0.00);

INSERT IGNORE INTO `trade_record`
(`trade_no`, `order_id`, `user_id`, `type`, `amount`, `available_after`, `frozen_after`, `remark`) VALUES
('TRSEED20240101000001', NULL, 2, 1, 1000.00, 1000.00, 0.00, '演示数据：初始充值'),
('TRSEED20240101000002', NULL, 3, 1, 500.00, 500.00, 0.00, '演示数据：初始充值');

-- ----------------------------------------
-- 商品（分类 1 数码 / 2 图书 / 3 生活 / 4 体育 / 5 宿舍 / 6 服装）
-- ----------------------------------------
INSERT IGNORE INTO `goods`
(`id`, `title`, `description`, `category_id`, `price`, `original_price`, `seller_id`, `images`, `status`, `view_count`) VALUES
(101, '九成新 iPad Air 5 64G 蓝色', '自用一年，无磕碰，配原装充电器，可当面验机。', 1, 2600.00, 4399.00, 3, '/uploads/demo/ipad.jpg', 1, 138),
(102, '罗技 G304 无线鼠标', '毕业出，功能完好，附赠一节电池。', 1, 89.00, 199.00, 3, '/uploads/demo/mouse.jpg', 1, 76),
(103, '《计算机网络：自顶向下方法》第 8 版', '只翻过前三章，无笔记无划线。', 2, 45.00, 89.00, 4, '/uploads/demo/book.jpg', 1, 54),
(104, '小米台灯 Pro', '宿舍用，色温可调，成色良好。', 3, 79.00, 169.00, 4, '/uploads/demo/lamp.jpg', 1, 41),
(105, '迪卡侬羽毛球拍一对', '含拍套，拉线还在保质期内。', 4, 60.00, 129.00, 3, '/uploads/demo/racket.jpg', 1, 33),
(106, '宿舍折叠小书桌', '毕业清仓，桌面无划痕。', 5, 35.00, 79.00, 4, '/uploads/demo/desk.jpg', 1, 28),
(107, '优衣库轻型羽绒服 M 码', '穿过两次，已干洗。', 6, 120.00, 399.00, 3, '/uploads/demo/jacket.jpg', 1, 22),
(108, '机械键盘 87 键 茶轴', '已售出，仅作历史数据展示。', 1, 150.00, 299.00, 3, '/uploads/demo/keyboard.jpg', 3, 96);

-- ----------------------------------------
-- 订单：两笔待付款（可演示支付/取消/超时），一笔已取消
-- ----------------------------------------
INSERT IGNORE INTO `orders`
(`id`, `order_no`, `buyer_id`, `seller_id`, `goods_id`, `amount`, `status`, `remark`) VALUES
(201, 'CT20240101000001', 2, 3, 102, 89.00, 0, '演示数据：待付款'),
(202, 'CT20240101000002', 2, 4, 103, 45.00, 0, '演示数据：待付款'),
(203, 'CT20240101000003', 4, 3, 105, 60.00, 4, '演示数据：已取消');