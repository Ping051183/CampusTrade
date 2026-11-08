-- ========================================
-- v3 迁移脚本：订单超时自动取消
-- 执行：mysql -u root -p campus_trade < backend/db/migration_v3.sql
-- ========================================

-- 定时任务扫描「待付款且已超时」的订单，联合索引避免全表扫描
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'orders'
      AND index_name = 'idx_status_create_time'
);
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE orders ADD INDEX idx_status_create_time (status, create_time)',
    'SELECT ''index already exists'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;