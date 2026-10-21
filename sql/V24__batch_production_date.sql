-- ============================================================
-- V24 批次生产日期
-- 背景：企业需要"扫码进通用页 → 选择批次 → 看该批次的检测报告与生产日期"。
--      此前生产日期只存在于 order_code.production_time（按码段分配，一个批次可能对应多条码段），
--      批次维度没有权威生产日期，无法作为批次列表的排序与展示依据。
-- 方案：batch 表新增 production_date，存量按批次已绑定码段的最新生产时间回填。
-- 幂等：ADD COLUMN 前先判断列是否存在；回填只覆盖仍为空的行
-- ============================================================

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'batch' AND COLUMN_NAME = 'production_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE batch ADD COLUMN production_date DATE NULL COMMENT ''生产日期'' AFTER test_time',
    'SELECT ''batch.production_date 已存在，跳过''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填：取该批次已绑定码段中最晚的生产时间（多次分批发码时以最近一次为准）
UPDATE batch b
JOIN (
    SELECT batch_id, MAX(DATE(production_time)) AS production_date
    FROM order_code
    WHERE batch_id IS NOT NULL AND production_time IS NOT NULL
    GROUP BY batch_id
) src ON src.batch_id = b.id
SET b.production_date = src.production_date
WHERE b.production_date IS NULL;
