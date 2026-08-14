-- ============================================================
-- V16 数字标签模块
-- ============================================================

-- 数字标签商品表
CREATE TABLE IF NOT EXISTS dl_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  food_name VARCHAR(200) NOT NULL COMMENT '食品名称',
  barcode VARCHAR(50) NOT NULL COMMENT '商品条码',
  spec VARCHAR(100) COMMENT '规格',
  label_version_count INT DEFAULT 0 COMMENT '数字标签版本数量',
  sync_status VARCHAR(20) DEFAULT 'synced' COMMENT '同步状态',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_enterprise (enterprise_id),
  INDEX idx_barcode (barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字标签商品表';

-- 数字标签版本表
CREATE TABLE IF NOT EXISTS dl_label_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  version_no VARCHAR(50) NOT NULL COMMENT '版本号(自动生成:日期+序号)',
  food_name VARCHAR(200),
  barcode VARCHAR(50),
  ingredients TEXT COMMENT '配料表',
  spec VARCHAR(100) COMMENT '规格',
  net_content VARCHAR(100) COMMENT '净含量',
  food_images TEXT COMMENT '食品图片(逗号分隔)',
  nutrition_image VARCHAR(500) COMMENT '营养成分表图片',
  food_category VARCHAR(200) COMMENT '食品分类',
  shelf_life VARCHAR(100) COMMENT '保质期',
  production_date_label VARCHAR(200) COMMENT '生产日期标示',
  expiry_date_label VARCHAR(200) COMMENT '保质期到期日标示',
  license_no VARCHAR(100) COMMENT '食品生产许可证编号',
  standard_code VARCHAR(100) COMMENT '产品标准代号',
  quality_grade VARCHAR(100) COMMENT '质量等级',
  storage_condition VARCHAR(200) COMMENT '贮存条件',
  gmo_food VARCHAR(10) COMMENT '转基因食品(是/否)',
  irradiated_food VARCHAR(10) COMMENT '辐照食品(是/否)',
  quantity_label VARCHAR(200) COMMENT '定量标识',
  batch_no_label VARCHAR(200) COMMENT '批号标示',
  allergens TEXT COMMENT '致敏物质',
  consumption_method TEXT COMMENT '食用方法',
  intro_video VARCHAR(500) COMMENT '食品介绍视频',
  certificates TEXT COMMENT '资质证书附件(逗号分隔)',
  custom_fields JSON COMMENT '自定义扩展字段',
  production_info JSON COMMENT '生产信息(JSON数组)',
  version_desc TEXT COMMENT '版本描述',
  status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/published/offline',
  published_at DATETIME COMMENT '发布时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_product (product_id),
  INDEX idx_barcode (barcode),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字标签版本表';

-- 扫码记录表
CREATE TABLE IF NOT EXISTS dl_scan_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  scan_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  location_province VARCHAR(50) COMMENT '省份',
  location_city VARCHAR(50) COMMENT '城市',
  ip VARCHAR(50),
  user_agent VARCHAR(500),
  INDEX idx_version (version_id),
  INDEX idx_product (product_id),
  INDEX idx_time (scan_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字标签扫码记录表';

-- 商品同步记录表
CREATE TABLE IF NOT EXISTS dl_sync_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  sync_type VARCHAR(20) COMMENT 'auto/manual',
  sync_condition VARCHAR(200) COMMENT '同步条件',
  time_range VARCHAR(50) COMMENT '时间范围',
  total_count INT DEFAULT 0,
  same_count INT DEFAULT 0,
  new_count INT DEFAULT 0,
  update_count INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'running' COMMENT 'running/success/failed',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品同步记录表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS dl_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  product_name VARCHAR(200),
  version_name VARCHAR(100),
  version_code VARCHAR(50),
  operation_type VARCHAR(50) COMMENT '创建版本/更新版本/删除版本/发布/下架',
  before_data JSON COMMENT '修改前数据',
  after_data JSON COMMENT '修改后数据',
  creator VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_enterprise (enterprise_id),
  INDEX idx_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字标签操作日志表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS dl_login_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  username VARCHAR(100),
  login_type VARCHAR(20) COMMENT 'PC/mobile',
  country VARCHAR(50),
  province VARCHAR(50),
  city VARCHAR(50),
  login_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_enterprise (enterprise_id),
  INDEX idx_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字标签登录日志表';

-- 食品分类表(预置数据)
CREATE TABLE IF NOT EXISTS dl_food_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT DEFAULT 0,
  name VARCHAR(100) NOT NULL,
  full_path VARCHAR(500) COMMENT '完整路径',
  sort_order INT DEFAULT 0,
  INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食品分类表';

-- ============================================================
-- 预置食品分类数据
-- ============================================================
INSERT INTO dl_food_category (id, parent_id, name, full_path, sort_order) VALUES
(1, 0, '通用', '通用', 1),
(2, 0, '乳制品', '乳制品', 2),
(3, 0, '谷物豆类', '谷物豆类', 3),
(4, 0, '肉制品', '肉制品', 4),
(5, 0, '饮品', '饮品', 5),
-- 通用
(101, 1, '休闲食品', '通用/休闲食品', 1),
(102, 1, '调味品', '通用/调味品', 2),
(103, 1, '罐头食品', '通用/罐头食品', 3),
(104, 1, '冷冻食品', '通用/冷冻食品', 4),
(105, 1, '其他', '通用/其他', 5),
-- 乳制品
(201, 2, '液态奶', '乳制品/液态奶', 1),
(202, 2, '酸奶', '乳制品/酸奶', 2),
(203, 2, '奶粉', '乳制品/奶粉', 3),
(204, 2, '奶酪', '乳制品/奶酪', 4),
(205, 2, '其他乳制品', '乳制品/其他乳制品', 5),
-- 谷物豆类
(301, 3, '大米', '谷物豆类/大米', 1),
(302, 3, '面粉及面制品', '谷物豆类/面粉及面制品', 2),
(303, 3, '杂粮', '谷物豆类/杂粮', 3),
(304, 3, '豆类及豆制品', '谷物豆类/豆类及豆制品', 4),
(305, 3, '其他谷物', '谷物豆类/其他谷物', 5),
-- 肉制品
(401, 4, '猪肉制品', '肉制品/猪肉制品', 1),
(402, 4, '牛肉制品', '肉制品/牛肉制品', 2),
(403, 4, '禽肉制品', '肉制品/禽肉制品', 3),
(404, 4, '水产制品', '肉制品/水产制品', 4),
(405, 4, '其他肉制品', '肉制品/其他肉制品', 5),
-- 饮品
(501, 5, '饮用水', '饮品/饮用水', 1),
(502, 5, '果蔬汁', '饮品/果蔬汁', 2),
(503, 5, '茶饮料', '饮品/茶饮料', 3),
(504, 5, '碳酸饮料', '饮品/碳酸饮料', 4),
(505, 5, '酒类', '饮品/酒类', 5),
(506, 5, '其他饮品', '饮品/其他饮品', 6);
