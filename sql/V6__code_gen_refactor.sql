-- V6: 码生成重构 - 防伪码固定长度

-- code_package 新增 anti_fake_digits 字段
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE code_package ADD COLUMN anti_fake_digits INT DEFAULT 10 COMMENT ''防伪码位数'' AFTER url_prefix',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema='trace_system' AND table_name='code_package' AND column_name='anti_fake_digits');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
