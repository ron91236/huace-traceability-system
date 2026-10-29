-- 模板体系重构 P2：长图内容所有权反转给模板。
-- 1) 放开 trace_long_module.name 的非空约束（新行不再写它，模块名归 config_json）
-- 2) long-image 模板的 config_json 补 schemaVersion + 空骨架（已有 longModules 的行不动）
-- 3) 备份并清空 trace_long_module：现存 3 行是企业自助建的验证数据，含第三方注册商标，无迁移价值
-- 沿用 V27 的 information_schema 探测 + PREPARE/EXECUTE 幂等写法，重复执行不报错。
SET NAMES utf8mb4;

-- ---------- 1) name 允许 NULL ----------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_long_module'
      AND COLUMN_NAME = 'name' AND IS_NULLABLE = 'NO');
SET @ddl = IF(@c = 1,
    'ALTER TABLE trace_long_module MODIFY COLUMN name VARCHAR(50) NULL COMMENT ''旧列：P2 起不写入，模块名归 config_json.longModules[].name''',
    'SELECT ''trace_long_module.name 已可为空，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2) 长图模板补骨架字段（只补，不覆盖已有 longModules） ----------
UPDATE trace_template
SET config_json = JSON_UNQUOTE(JSON_SET(
        CAST(config_json AS JSON), '$.schemaVersion', 2, '$.longModules', JSON_ARRAY()))
WHERE JSON_VALID(COALESCE(config_json, '')) = 1
  AND JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.renderMode')) = 'long-image'
  AND JSON_CONTAINS_PATH(config_json, 'one', '$.longModules') = 0;

-- ---------- 3) truncate 前先留一份可查的备份 ----------
SET @t = (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_long_module_bak_v28');
SET @ddl = IF(@t = 0,
    'CREATE TABLE trace_long_module_bak_v28 AS SELECT * FROM trace_long_module',
    'SELECT ''trace_long_module_bak_v28 已存在，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @n = (SELECT COUNT(*) FROM trace_long_module);
SET @ddl = IF(@n > 0, 'TRUNCATE TABLE trace_long_module', 'SELECT ''trace_long_module 已是空表，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
