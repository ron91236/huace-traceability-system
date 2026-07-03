-- V7: 溯源码作废管理

CREATE TABLE IF NOT EXISTS `voided_code_range` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code_package_id` BIGINT DEFAULT NULL COMMENT '条码库(码包ID)',
  `serial_digits` INT NOT NULL DEFAULT 10 COMMENT '流水号位数',
  `serial_start` VARCHAR(50) NOT NULL COMMENT '开始身份码',
  `serial_end` VARCHAR(50) NOT NULL COMMENT '结束身份码',
  `count` INT NOT NULL DEFAULT 0 COMMENT '标签数量',
  `remark` VARCHAR(500) COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_package_id` (`code_package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源码作废范围表';
