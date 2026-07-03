-- V9: order_code 表新增 bind_count 字段
ALTER TABLE `order_code` ADD COLUMN `bind_count` INT DEFAULT NULL
  COMMENT '实际绑定数量(总数-作废数)' AFTER `waste_count`;
