-- V8: 证书产品 + 产能管理

CREATE TABLE IF NOT EXISTS `cert_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cert_id` BIGINT NOT NULL COMMENT '关联证书ID',
  `product_id` BIGINT DEFAULT NULL COMMENT '关联产品ID',
  `product_name` VARCHAR(128) COMMENT '产品名称(冗余)',
  `total_production` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '企业总产量(吨)',
  `remaining_production` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '企业剩余产量(吨)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cert_id` (`cert_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书产品产能表';
