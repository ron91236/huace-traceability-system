-- 扩展 goods 表图片字段长度，支持多张图片URL存储
ALTER TABLE goods MODIFY COLUMN sample_image VARCHAR(2000) COMMENT '样品图片(多张逗号分隔)';
ALTER TABLE goods MODIFY COLUMN promo_image VARCHAR(2000) COMMENT '宣传图片(多张逗号分隔)';
