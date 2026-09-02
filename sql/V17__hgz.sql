-- ============================================================
-- V17 农产品承诺达标合格证模块
-- 政策依据：《农产品质量安全承诺达标合格证管理办法》农业农村部令2025年第4号（2026-02-01施行）
-- 字段对齐法定要素：产品名称/数量(重量)/产地/承诺主体/联系方式/开具日期/承诺事项/承诺依据/二维码
-- ============================================================

CREATE TABLE IF NOT EXISTS hgz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL COMMENT '合格证号(唯一)',
    enterprise_id BIGINT NOT NULL COMMENT '承诺主体企业ID',
    batch_id BIGINT DEFAULT NULL COMMENT '关联批次ID(与溯源打通)',
    goods_id BIGINT DEFAULT NULL COMMENT '关联商品ID',
    user_type TINYINT NOT NULL DEFAULT 1 COMMENT '主体类型: 1=生产者 2=收购者',
    product_name VARCHAR(200) NOT NULL COMMENT '产品名称',
    number VARCHAR(100) DEFAULT NULL COMMENT '数量(重量)',
    place_of_origin VARCHAR(255) DEFAULT NULL COMMENT '产地',
    promise_user VARCHAR(200) NOT NULL COMMENT '承诺主体名称(冗余存储,防企业改名影响历史证)',
    contact VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
    use_time DATE DEFAULT NULL COMMENT '开具日期',
    signature VARCHAR(500) DEFAULT NULL COMMENT '签名/盖章图片路径',
    promise_list TEXT COMMENT '承诺事项JSON [{title,isSelect}]',
    basis_list TEXT COMMENT '承诺依据JSON [{title,isSelect,image}]',
    is_show_enterprise TINYINT NOT NULL DEFAULT 1 COMMENT '是否展示企业信息',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=有效 0=作废',
    qr_url VARCHAR(500) DEFAULT NULL COMMENT '合格证单独链接',
    query_url VARCHAR(500) DEFAULT NULL COMMENT '溯源回链地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_enterprise (enterprise_id),
    KEY idx_batch (batch_id),
    KEY idx_goods (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='农产品承诺达标合格证';
