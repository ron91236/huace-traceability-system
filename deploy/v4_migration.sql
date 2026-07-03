-- Phase 4 Migration Script (MySQL 8.0 compatible)

-- 1. Test Report table
CREATE TABLE IF NOT EXISTS `test_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL,
  `report_name` VARCHAR(128) NOT NULL,
  `test_code` VARCHAR(64),
  `report_image` VARCHAR(255),
  `report_pdf` VARCHAR(255),
  `test_org` VARCHAR(128),
  `test_time` DATETIME,
  `test_method` VARCHAR(128),
  `test_basis` VARCHAR(255),
  `test_type` VARCHAR(64),
  `test_result` VARCHAR(64),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Scan record table
CREATE TABLE IF NOT EXISTS `scan_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `serial_no` VARCHAR(32) NOT NULL,
  `enterprise_id` BIGINT,
  `province` VARCHAR(32),
  `city` VARCHAR(32),
  `ip` VARCHAR(64),
  `user_agent` VARCHAR(512),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_serial` (`serial_no`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Enterprise template data table
CREATE TABLE IF NOT EXISTS `enterprise_template_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL,
  `template_id` BIGINT NOT NULL,
  `field_key` VARCHAR(128) NOT NULL,
  `field_label` VARCHAR(128),
  `field_value` TEXT,
  `field_type` VARCHAR(32) DEFAULT 'text',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ent_tpl_field` (`enterprise_id`, `template_id`, `field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
