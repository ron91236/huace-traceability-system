-- 侧边栏链接支持多份资料（多张图片 / 多个多页 PDF）：
-- enterprise_side_link.image 从"单个路径 VARCHAR(500)"改成"路径的 JSON 数组 TEXT"。
-- 语义与 trace_landing_content.promoImages 一致：JSON 字符串数组，NULL = 没配资料。
-- 沿用 V27/V28 的 information_schema 探测 + PREPARE/EXECUTE 幂等写法，重复执行不报错。
SET NAMES utf8mb4;

-- ---------- 1) image 扩容成 TEXT ----------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enterprise_side_link'
      AND COLUMN_NAME = 'image' AND DATA_TYPE <> 'text');
SET @ddl = IF(@c = 1,
    'ALTER TABLE enterprise_side_link MODIFY COLUMN image TEXT COMMENT ''跳转资料：图片或 PDF 的 /uploads/ 路径 JSON 数组；NULL=没配''',
    'SELECT ''enterprise_side_link.image 已是 TEXT，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2) 现存裸路径回填成单元素数组 ----------
-- 读侧仍保留裸路径兼容（SideLinkService.readItems），回填只是为了库里不留两种表示。
UPDATE enterprise_side_link
SET image = JSON_ARRAY(TRIM(image))
WHERE image IS NOT NULL AND TRIM(image) <> '' AND LEFT(TRIM(image), 1) <> '[';
