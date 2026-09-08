-- ============================================================
-- V19 码包下发锁定机制 + 审计日志
-- 码包下发印刷厂后禁止删除/修改，解除需填原因并留审计记录
-- ============================================================

ALTER TABLE code_package
    ADD COLUMN dispatched TINYINT(1) DEFAULT 0 COMMENT '是否已下发印刷厂(0=未下发,1=已下发)',
    ADD COLUMN dispatched_at DATETIME COMMENT '最近一次下发时间',
    ADD COLUMN dispatched_by VARCHAR(100) COMMENT '最近一次下发操作人',
    ADD COLUMN undispatched_at DATETIME COMMENT '最近一次解除下发时间',
    ADD COLUMN undispatched_by VARCHAR(100) COMMENT '最近一次解除下发操作人',
    ADD COLUMN undispatch_reason VARCHAR(500) COMMENT '最近一次解除下发原因';

CREATE TABLE code_package_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL COMMENT '码包ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型: DISPATCH/UNDISPATCH',
    operator_name VARCHAR(100) COMMENT '操作人',
    reason VARCHAR(500) COMMENT '操作原因(解除下发时必填)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_package_time (package_id, created_at)
) COMMENT='码包下发操作审计日志';
