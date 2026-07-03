-- V11: 检测报告功能升级 - 多图支持 + 批次多报告关联

-- 1. 扩展 report_image 为 TEXT 支持多图(逗号分隔URL)
ALTER TABLE test_report MODIFY report_image TEXT COMMENT '报告图片(逗号分隔多URL)';
ALTER TABLE test_report MODIFY report_pdf VARCHAR(500) COMMENT '报告PDF URL';

-- 2. 批次-检测报告 多对多关联表
CREATE TABLE IF NOT EXISTS `batch_test_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `batch_id` BIGINT NOT NULL,
  `test_report_id` BIGINT NOT NULL,
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_report` (`batch_id`, `test_report_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_test_report_id` (`test_report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次-检测报告关联表';

-- 3. 数据迁移：将现有 batch.test_report_id 数据写入中间表
INSERT INTO batch_test_report (batch_id, test_report_id, sort_order)
SELECT id, test_report_id, 0 FROM batch WHERE test_report_id IS NOT NULL;
