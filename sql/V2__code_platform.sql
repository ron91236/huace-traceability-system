-- 码中台功能 - 码包生成扩展
-- ALTER TABLE code_package 添加码包生成相关字段

ALTER TABLE `code_package`
  ADD COLUMN `product_code` VARCHAR(32) COMMENT '产品代码' AFTER `source_file`,
  ADD COLUMN `year_code` VARCHAR(8) COMMENT '年份代码' AFTER `product_code`,
  ADD COLUMN `serial_digits` INT DEFAULT 8 COMMENT '流水号位数' AFTER `year_code`,
  ADD COLUMN `serial_start` BIGINT COMMENT '起始流水号' AFTER `serial_digits`,
  ADD COLUMN `serial_end` BIGINT COMMENT '终止流水号' AFTER `serial_start`,
  ADD COLUMN `start_quantity` BIGINT COMMENT '起始数量(全局计数)' AFTER `serial_end`,
  ADD COLUMN `code_type` VARCHAR(32) DEFAULT 'SERIAL_URL_ANTI' COMMENT '生码类型: ANTI_ONLY / SERIAL_URL_ANTI' AFTER `start_quantity`,
  ADD COLUMN `url_prefix` VARCHAR(255) DEFAULT 'http://cti.cti-pit.com/?c=' COMMENT '溯源码前缀' AFTER `code_type`,
  ADD COLUMN `remark` VARCHAR(500) COMMENT '备注内容' AFTER `url_prefix`,
  ADD COLUMN `source_type` VARCHAR(20) DEFAULT 'IMPORT' COMMENT '来源: IMPORT=导入 / GENERATE=生成' AFTER `remark`;
