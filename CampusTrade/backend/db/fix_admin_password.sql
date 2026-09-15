-- ========================================
-- 修复管理员密码哈希
-- 密码: admin123
-- 修复 init.sql 中错误的 BCrypt 哈希
-- ========================================

-- 如果管理员已存在，更新密码
UPDATE `user`
SET `password` = '$2a$10$GuRwu7PRfefHUWAvVFiGCehjMvW.udODd/nHx9cl9YVX15Z9oDagW'
WHERE `username` = 'admin';

-- 如果管理员不存在，插入
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`)
SELECT 'admin', '$2a$10$GuRwu7PRfefHUWAvVFiGCehjMvW.udODd/nHx9cl9YVX15Z9oDagW', '系统管理员', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'admin');

SELECT '管理员密码已修复，密码: admin123' AS result;
