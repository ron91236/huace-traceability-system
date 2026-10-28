-- 模板体系重构 P1：权限地基建表。纯增量——不删列、不删数据、不改 config_json。
-- 破坏性清理（TRUNCATE trace_long_module、改 long-image 模板 config_json）在 P2。
SET NAMES utf8mb4;

-- ---------- 1) trace_long_module 加 template_id ----------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_long_module' AND COLUMN_NAME = 'template_id');
SET @ddl = IF(@c = 0,
    'ALTER TABLE trace_long_module ADD COLUMN template_id BIGINT NULL COMMENT ''归属模板；P2 起启用，P1 期间为 NULL'' AFTER id',
    'SELECT ''trace_long_module.template_id 已存在，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2) trace_long_module 加 module_key ----------
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_long_module' AND COLUMN_NAME = 'module_key');
SET @ddl = IF(@c = 0,
    'ALTER TABLE trace_long_module ADD COLUMN module_key VARCHAR(64) NULL COMMENT ''对应 config_json.longModules[].key；P2 起启用'' AFTER template_id',
    'SELECT ''trace_long_module.module_key 已存在，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 3) 唯一键 ----------
-- 两列都可空，MySQL 唯一索引里 NULL 互不相等，所以存量行（template_id/module_key 均为 NULL）
-- 不会互相冲突，加索引不会失败。
SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_long_module' AND INDEX_NAME = 'uk_tpl_ent_mod');
SET @ddl = IF(@c = 0,
    'ALTER TABLE trace_long_module ADD UNIQUE KEY uk_tpl_ent_mod (template_id, enterprise_id, module_key)',
    'SELECT ''uk_tpl_ent_mod 已存在，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 4) 落地页区块内容表（P4 使用，本阶段只建表）----------
SET @c = (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trace_landing_content');
SET @ddl = IF(@c = 0,
    'CREATE TABLE trace_landing_content (
        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键'',
        template_id BIGINT NOT NULL COMMENT ''归属落地页模板'',
        enterprise_id BIGINT NOT NULL COMMENT ''填写内容的企业'',
        block_key VARCHAR(32) NOT NULL COMMENT ''区块类型：richtext/video/promoImages/antiFake'',
        content MEDIUMTEXT COMMENT ''区块内容：richtext 存 HTML，video 存 URL，promoImages 存 URL 的 JSON 数组；antiFake 不写行'',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
        UNIQUE KEY uk_tpl_ent_blk (template_id, enterprise_id, block_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''静态落地页区块内容：模板定骨架，企业填内容''',
    'SELECT ''trace_landing_content 已存在，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 5) 企业侧边栏外链表（P4 使用，挂企业不挂模板）----------
SET @c = (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enterprise_side_link');
SET @ddl = IF(@c = 0,
    'CREATE TABLE enterprise_side_link (
        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''主键'',
        enterprise_id BIGINT NOT NULL COMMENT ''归属企业；挂企业而非模板，换模板不丢配置'',
        name VARCHAR(50) NOT NULL COMMENT ''按钮文案，即侧边栏显示文字'',
        icon VARCHAR(64) COMMENT ''图标 class 或图标标识，可空则用默认图标'',
        url VARCHAR(500) COMMENT ''跳转链接；非空时优先跳转'',
        image VARCHAR(500) COMMENT ''跳转图片（公众号二维码等）；url 为空时点开弹层显示'',
        sort_order INT NOT NULL DEFAULT 0 COMMENT ''按钮顺序，小者在前'',
        status TINYINT NOT NULL DEFAULT 1 COMMENT ''1启用 0停用'',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
        INDEX idx_ent_status_sort (enterprise_id, status, sort_order)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''企业侧边栏外链：公众号/微博等，与模板无关的企业固有资产''',
    'SELECT ''enterprise_side_link 已存在，跳过''');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
