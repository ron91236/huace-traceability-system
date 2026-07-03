-- 亿级码包场景查询优化索引

-- code_package_item: 按企业+绑定状态统计、按码包ID分页查询
CREATE INDEX idx_cpi_enterprise_bind ON code_package_item (enterprise_id, bind_status);
CREATE INDEX idx_cpi_package_serial ON code_package_item (package_id, serial_no);
CREATE INDEX idx_cpi_bind_time ON code_package_item (bind_time);

-- 当码包迁移到 MongoDB 后，MySQL 中保留的汇总表索引（如已迁移可忽略）
-- code_package: 按企业查询其生成的码包
CREATE INDEX idx_code_package_enterprise ON code_package (enterprise_id, created_at);

-- trace_template: 按类型和状态查询启用的模板
CREATE INDEX idx_trace_template_type_status ON trace_template (template_type, status);

-- trace_page_record: 扫码记录按流水号和时间范围查询
CREATE INDEX idx_tpr_serial_time ON trace_page_record (serial_no, created_at);
CREATE INDEX idx_tpr_enterprise_time ON trace_page_record (enterprise_id, created_at);

-- batch: 按企业查询批次
CREATE INDEX idx_batch_enterprise_id ON batch (enterprise_id, created_at);

-- order_code: 按企业查询订单条码绑定情况
CREATE INDEX idx_order_code_enterprise ON order_code (enterprise_id, bind_status);
