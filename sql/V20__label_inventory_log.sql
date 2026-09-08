-- V20: 标签库存日志表
-- 用于记录标签库存的变动（入库/作废扣减/发货扣减）

CREATE TABLE label_inventory_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code_package_id BIGINT COMMENT '码包ID，入库时必填',
  label_spec_id BIGINT COMMENT '标签规格ID',
  change_type VARCHAR(20) NOT NULL COMMENT '变动类型: IN(入库)/VOID(作废扣减)/SHIP(发货扣减)',
  quantity INT NOT NULL COMMENT '变动数量（正数）',
  serial_start VARCHAR(50) COMMENT '起始序列号（作废/发货时记录）',
  serial_end VARCHAR(50) COMMENT '结束序列号（作废/发货时记录）',
  order_id BIGINT COMMENT '订单ID（发货时记录）',
  order_code_id BIGINT COMMENT '订单码ID（发货时记录）',
  voided_range_id BIGINT COMMENT '作废记录ID（作废时记录）',
  remark VARCHAR(500) COMMENT '备注',
  operator_name VARCHAR(100) COMMENT '操作人',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_package_id (code_package_id),
  INDEX idx_label_spec_id (label_spec_id),
  INDEX idx_change_type (change_type),
  INDEX idx_created_at (created_at)
) COMMENT='标签库存变动日志';
