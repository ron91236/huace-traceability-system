-- V12: 企业资质证书字段由 JSON 改为 TEXT
-- 背景：前端以纯文本/富文本 HTML 提交 qualifications，写入 json 列导致
-- "Invalid JSON text" 保存失败（营业执照/形象图/视频等一并无法保存）
-- 该字段历史数据均为 NULL，无存量数据需转换
ALTER TABLE enterprise MODIFY COLUMN qualifications TEXT COMMENT '资质证书（富文本）';
