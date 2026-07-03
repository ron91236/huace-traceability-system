-- V5: 订单条码绑定 + 废弃码管理

-- 1. order_code 新增字段
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE order_code ADD COLUMN code_package_id BIGINT DEFAULT NULL COMMENT ''码包ID'' AFTER order_id',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema='trace_system' AND table_name='order_code' AND column_name='code_package_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE order_code ADD COLUMN waste_count INT DEFAULT 0 COMMENT ''作废数量'' AFTER serial_end',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema='trace_system' AND table_name='order_code' AND column_name='waste_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 废弃码记录表
CREATE TABLE IF NOT EXISTS `waste_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code_package_id` BIGINT NOT NULL COMMENT '所属码包',
  `serial_start` VARCHAR(50) COMMENT '起始码',
  `serial_end` VARCHAR(50) COMMENT '结束码',
  `count` INT DEFAULT 0 COMMENT '作废数量',
  `reason` VARCHAR(255) COMMENT '原因',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_package_id` (`code_package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='废弃码记录表';

-- 3. code_package_item 增加 WASTE 绑定状态支持（已有 UNBOUND/BOUND，新增标记）
-- waste 状态通过 waste_code 表追踪，code_package_item 的 bind_status 设为 WASTE
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE code_package_item ADD COLUMN remark VARCHAR(255) COMMENT ''备注''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema='trace_system' AND table_name='code_package_item' AND column_name='remark');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
