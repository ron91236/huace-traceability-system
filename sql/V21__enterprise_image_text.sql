-- ============================================================
-- V21 企业形象图支持多张
-- enterprise_image 原为 varchar(255)，多张图片的 URL 逗号拼接后会被截断，
-- 扩为 TEXT 以容纳最多 9 张图片地址
-- ============================================================

ALTER TABLE enterprise
    MODIFY COLUMN enterprise_image TEXT COMMENT '企业形象图(多张以逗号分隔)';
