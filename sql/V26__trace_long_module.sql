-- 长图溯源模板：企业自定义模块（整页图自由拼接）+ long-image 模板行
SET @tbl_exists = (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_long_module');
SET @ddl = IF(@tbl_exists = 0,
    'CREATE TABLE trace_long_module (
        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键'',
        enterprise_id BIGINT NOT NULL COMMENT ''归属企业'',
        goods_id BIGINT NULL COMMENT ''归属商品，NULL=企业默认全部商品'',
        name VARCHAR(50) NOT NULL COMMENT ''模块名称，即底部导航文案'',
        pages TEXT NOT NULL COMMENT ''整页图片URL的JSON数组，顺序即拼接顺序'',
        sort_order INT NOT NULL DEFAULT 0 COMMENT ''导航顺序，小者在前'',
        status TINYINT NOT NULL DEFAULT 1 COMMENT ''1启用 0停用'',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
        INDEX idx_ent_goods (enterprise_id, goods_id, status, sort_order)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''长图溯源模块：企业自助配置的整屏长图内容''',
    'SELECT ''trace_long_module 已存在，跳过''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 模板行：只承载样式与渲染模式，不含企业内容；template_key 唯一，重复执行不报错
INSERT INTO trace_template (template_key, template_name, template_type, config_json, status)
SELECT 'long-image', '长图溯源', 'custom', '{"renderMode":"long-image"}', 1
WHERE NOT EXISTS (SELECT 1 FROM trace_template WHERE template_key = 'long-image');
