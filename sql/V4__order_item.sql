-- V4: 订单明细表重构 - 匹配标签规格+批次设计

-- 删除旧字段（如果存在）
SET @sql = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE order_item DROP COLUMN tag_count', 'SELECT 1') FROM information_schema.columns WHERE table_schema='trace_system' AND table_name='order_item' AND column_name='tag_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE order_item DROP COLUMN unit_price', 'SELECT 1') FROM information_schema.columns WHERE table_schema='trace_system' AND table_name='order_item' AND column_name='unit_price');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 新增字段（如果不存在）
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE order_item ADD COLUMN batch_id BIGINT DEFAULT NULL COMMENT ''批次ID'' AFTER order_id', 'SELECT 1') FROM information_schema.columns WHERE table_schema='trace_system' AND table_name='order_item' AND column_name='batch_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE order_item ADD COLUMN goods_weight VARCHAR(64) DEFAULT '''' COMMENT ''重量'' AFTER goods_spec', 'SELECT 1') FROM information_schema.columns WHERE table_schema='trace_system' AND table_name='order_item' AND column_name='goods_weight');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE order_item ADD COLUMN label_spec_material VARCHAR(200) DEFAULT '''' COMMENT ''材质'' AFTER label_spec_name', 'SELECT 1') FROM information_schema.columns WHERE table_schema='trace_system' AND table_name='order_item' AND column_name='label_spec_material');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE order_item ADD COLUMN label_spec_type VARCHAR(64) DEFAULT '''' COMMENT ''类型'' AFTER label_spec_material', 'SELECT 1') FROM information_schema.columns WHERE table_schema='trace_system' AND table_name='order_item' AND column_name='label_spec_type');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE order_item ADD COLUMN price DECIMAL(10,4) DEFAULT NULL COMMENT ''价格(来自标签规格)'' AFTER label_spec_type', 'SELECT 1') FROM information_schema.columns WHERE table_schema='trace_system' AND table_name='order_item' AND column_name='price');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 修复 audit_log 表 operator_id 允许为空
ALTER TABLE audit_log MODIFY COLUMN operator_id BIGINT DEFAULT NULL COMMENT '操作人ID';
