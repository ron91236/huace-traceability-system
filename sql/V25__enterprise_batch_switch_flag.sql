-- 溯源页批次切换：企业个性化定制权限开关
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enterprise' AND COLUMN_NAME = 'batch_switch_enabled');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE enterprise ADD COLUMN batch_switch_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''溯源页批次切换个性化定制(0关闭/1开启)'' AFTER anti_fake_input_hint',
    'SELECT ''enterprise.batch_switch_enabled 已存在，跳过''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
