-- 产品溯源系统数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS `trace_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `trace_system`;

-- 角色表
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `role_type` VARCHAR(20) DEFAULT 'admin' COMMENT 'admin/enterprise',
  `permissions` JSON COMMENT '权限列表',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户表
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
  `nickname` VARCHAR(64) COMMENT '昵称',
  `avatar` VARCHAR(255) COMMENT '头像路径',
  `email` VARCHAR(120) COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `role_id` BIGINT COMMENT '角色ID',
  `enterprise_id` BIGINT COMMENT '所属企业ID',
  `user_type` VARCHAR(20) DEFAULT 'admin' COMMENT 'admin/enterprise',
  `status` TINYINT DEFAULT 1 COMMENT '1=启用,0=禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 企业表
CREATE TABLE `enterprise` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT '企业名称',
  `nature` VARCHAR(64) COMMENT '企业性质',
  `industry` VARCHAR(64) COMMENT '所属行业',
  `contact` VARCHAR(64) COMMENT '联系人',
  `phone` VARCHAR(20) COMMENT '联系电话',
  `account_type` VARCHAR(20) COMMENT '种植/养殖/加工',
  `login_account` VARCHAR(64) COMMENT '登录账号',
  `login_password_hash` VARCHAR(255) COMMENT '登录密码',
  `status` TINYINT DEFAULT 1 COMMENT '1=启用,0=禁用',
  `credit_code` VARCHAR(64) COMMENT '统一社会信用代码',
  `email` VARCHAR(120) COMMENT '邮箱',
  `license_image` VARCHAR(255) COMMENT '营业执照图片',
  `enterprise_image` VARCHAR(255) COMMENT '企业照片',
  `main_type` VARCHAR(64) COMMENT '主营类型',
  `show_outer_package` TINYINT DEFAULT 0 COMMENT '是否展示外包装',
  `total_staff` INT COMMENT '员工总数',
  `province` VARCHAR(32) COMMENT '省',
  `city` VARCHAR(32) COMMENT '市',
  `district` VARCHAR(32) COMMENT '区',
  `address` VARCHAR(255) COMMENT '详细地址',
  `longitude` DECIMAL(10,7) COMMENT '经度',
  `latitude` DECIMAL(10,7) COMMENT '纬度',
  `honors` TEXT COMMENT '荣誉',
  `zipcode` VARCHAR(10) COMMENT '邮编',
  `standard_system` TEXT COMMENT '标准体系',
  `introduction` TEXT COMMENT '企业介绍',
  `qualifications` JSON COMMENT '企业资质',
  `seal_image` VARCHAR(255) COMMENT '公章图片',
  `promo_video` VARCHAR(255) COMMENT '宣传视频',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_login_account` (`login_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业表';

-- 证书类型表
CREATE TABLE `cert_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL COMMENT '类型名称',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证书类型表';

-- 企业认证表
CREATE TABLE `enterprise_cert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cert_type_id` BIGINT COMMENT '证书类型ID',
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业ID',
  `cert_name` VARCHAR(128) COMMENT '证书名称',
  `product_name` VARCHAR(128) COMMENT '产品名称',
  `start_date` DATE COMMENT '有效期开始',
  `end_date` DATE COMMENT '有效期结束',
  `is_void` TINYINT DEFAULT 0 COMMENT '是否作废',
  `cert_image` VARCHAR(255) COMMENT '证书图片',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_cert_type_id` (`cert_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业认证表';

-- 产品表（华测端维护的产品大类）
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT '产品名称',
  `description` TEXT COMMENT '描述',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- 标签规格表
CREATE TABLE `label_spec` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `spec_name` VARCHAR(64) NOT NULL COMMENT '规格名称',
  `material` VARCHAR(64) COMMENT '材质',
  `price` DECIMAL(10,2) DEFAULT 0 COMMENT '单价',
  `usage_method` VARCHAR(64) COMMENT '使用方式（贴标/挂牌等）',
  `support_manual_assign` TINYINT DEFAULT 0 COMMENT '是否支持手动指定流水号',
  `is_void` TINYINT DEFAULT 0 COMMENT '是否作废',
  `label_image` VARCHAR(255) COMMENT '标签图片',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签规格表';

-- 商品表（企业维护的具体商品）
CREATE TABLE `goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT COMMENT '所属产品大类',
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `name` VARCHAR(128) NOT NULL COMMENT '商品名称',
  `package_spec` VARCHAR(64) COMMENT '包装规格',
  `show_outer_package` TINYINT DEFAULT 0 COMMENT '展示外包装',
  `weight_spec` VARCHAR(64) COMMENT '重量规格',
  `sample_image` VARCHAR(255) COMMENT '样品图片',
  `introduction` TEXT COMMENT '商品介绍',
  `storage_method` VARCHAR(255) COMMENT '储存方式',
  `eating_method` VARCHAR(255) COMMENT '食用方式',
  `promo_image` VARCHAR(255) COMMENT '宣传图片',
  `promo_video` VARCHAR(255) COMMENT '宣传视频',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 企业基地表
CREATE TABLE `enterprise_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `name` VARCHAR(128) NOT NULL COMMENT '基地名称',
  `code` VARCHAR(64) COMMENT '基地编号',
  `area` DECIMAL(12,2) COMMENT '面积',
  `unit` VARCHAR(20) COMMENT '面积单位（亩/公顷）',
  `manager` VARCHAR(64) COMMENT '负责人',
  `phone` VARCHAR(20) COMMENT '联系电话',
  `plan_image` VARCHAR(255) COMMENT '平面图',
  `real_image` VARCHAR(255) COMMENT '实景图',
  `certification` VARCHAR(255) COMMENT '认证情况',
  `province` VARCHAR(32) COMMENT '省',
  `city` VARCHAR(32) COMMENT '市',
  `district` VARCHAR(32) COMMENT '区',
  `test_items` TEXT COMMENT '检测项目（JSON数组）',
  `env_report` VARCHAR(255) COMMENT '环境检测报告',
  `monitor_info` TEXT COMMENT '监控信息（JSON）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业基地表';

-- 批次表
CREATE TABLE `batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT '批次名称',
  `goods_id` BIGINT COMMENT '所属商品',
  `goods_spec` VARCHAR(64) COMMENT '商品规格',
  `base_id` BIGINT COMMENT '所属基地',
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `test_code` VARCHAR(64) COMMENT '检测编号',
  `test_report` VARCHAR(255) COMMENT '检测报告文件路径',
  `test_org` VARCHAR(128) COMMENT '检测机构',
  `test_time` DATETIME COMMENT '检测时间',
  `test_method` VARCHAR(128) COMMENT '检测方式',
  `test_basis` VARCHAR(255) COMMENT '检测依据',
  `test_type` VARCHAR(64) COMMENT '检测类型',
  `test_result` VARCHAR(64) COMMENT '检测结果',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_base_id` (`base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次表';

-- 收货地址表
CREATE TABLE `address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `contact` VARCHAR(64) COMMENT '联系人',
  `phone` VARCHAR(20) COMMENT '手机号',
  `address` VARCHAR(255) COMMENT '详细地址',
  `zipcode` VARCHAR(10) COMMENT '邮编',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 订单表（order是保留字，使用反引号）
CREATE TABLE `t_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `enterprise_id` BIGINT NOT NULL COMMENT '下单企业',
  `cert_id` BIGINT COMMENT '关联证书',
  `status` VARCHAR(10) DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED',
  `address_id` BIGINT COMMENT '收货地址',
  `attachment` VARCHAR(255) COMMENT '附件',
  `submit_time` DATETIME COMMENT '提交时间',
  `review_time` DATETIME COMMENT '审核时间',
  `reviewer_id` BIGINT COMMENT '审核人',
  `review_note` TEXT COMMENT '审核备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单条码表
CREATE TABLE `order_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '所属订单',
  `label_spec_id` BIGINT COMMENT '标签规格',
  `batch_id` BIGINT COMMENT '关联批次',
  `product_name` VARCHAR(128) COMMENT '产品名称',
  `trace_template` VARCHAR(64) COMMENT '溯源模板标识',
  `product_template` VARCHAR(64) COMMENT '产品模板标识',
  `quantity` INT DEFAULT 0 COMMENT '数量',
  `price` DECIMAL(10,2) DEFAULT 0 COMMENT '单价',
  `is_unsubscribed` TINYINT DEFAULT 0 COMMENT '是否退订',
  `serial_start` VARCHAR(20) COMMENT '起始流水号',
  `serial_end` VARCHAR(20) COMMENT '结束流水号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_label_spec_id` (`label_spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单条码表';

-- 码包主表
CREATE TABLE `code_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `package_no` VARCHAR(64) NOT NULL COMMENT '码包编号',
  `label_spec_id` BIGINT COMMENT '标签规格',
  `total_count` INT DEFAULT 0 COMMENT '码包总条数',
  `source_file` VARCHAR(255) COMMENT '原始导入文件路径',
  `status` VARCHAR(20) DEFAULT 'UNBOUND' COMMENT 'UNBOUND/PARTIAL/BOUND',
  `import_time` DATETIME COMMENT '导入时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_package_no` (`package_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='码包主表';

-- 码包明细表
CREATE TABLE `code_package_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `package_id` BIGINT NOT NULL COMMENT '所属码包',
  `serial_no` VARCHAR(32) NOT NULL COMMENT '流水号',
  `anti_fake_code` VARCHAR(64) COMMENT '防伪码',
  `url` VARCHAR(512) COMMENT '溯源网址（完整URL）',
  `order_code_id` BIGINT COMMENT '绑定的订单条码',
  `enterprise_id` BIGINT COMMENT '绑定的企业',
  `goods_id` BIGINT COMMENT '绑定的商品',
  `cert_id` BIGINT COMMENT '绑定的证书',
  `batch_id` BIGINT COMMENT '绑定的批次',
  `trace_template` VARCHAR(64) COMMENT '溯源模板标识',
  `bind_status` VARCHAR(20) DEFAULT 'UNBOUND' COMMENT 'UNBOUND/BOUND',
  `bind_time` DATETIME COMMENT '绑定时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_serial_anti` (`serial_no`, `anti_fake_code`),
  KEY `idx_package_id` (`package_id`),
  KEY `idx_bind_status` (`bind_status`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_serial_no` (`serial_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='码包明细表';

-- 溯源码入库/使用记录
CREATE TABLE `trace_inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code_pool` VARCHAR(64) COMMENT '码池标识',
  `label_spec_id` BIGINT COMMENT '标签规格',
  `start_serial` VARCHAR(20) COMMENT '起始流水号',
  `end_serial` VARCHAR(20) COMMENT '结束流水号',
  `produce_time` DATETIME COMMENT '生产时间',
  `enterprise_id` BIGINT COMMENT '所属企业',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源码入库/使用记录';

-- 公告表
CREATE TABLE `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL COMMENT '标题',
  `content` TEXT COMMENT '内容',
  `enterprise_id` BIGINT COMMENT '指定企业（NULL=全局）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 溯源模板表
CREATE TABLE `trace_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_key` VARCHAR(64) NOT NULL COMMENT '模板标识（如vegetable,fruit）',
  `template_name` VARCHAR(128) COMMENT '模板显示名称',
  `template_type` VARCHAR(20) COMMENT '模板类型（产品大类）',
  `preview_image` VARCHAR(255) COMMENT '模板预览图',
  `config_json` JSON COMMENT '模板配置（显示字段、排序、样式）',
  `status` TINYINT DEFAULT 1 COMMENT '1=启用,0=禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_key` (`template_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源模板表';

-- 轮播图/横幅表
CREATE TABLE `banner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(128) COMMENT '标题',
  `image_url` VARCHAR(255) COMMENT '图片',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '1=启用,0=禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- 文件记录表
CREATE TABLE `sys_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `original_name` VARCHAR(255) COMMENT '原始文件名',
  `stored_name` VARCHAR(255) COMMENT '存储文件名（UUID）',
  `file_path` VARCHAR(512) COMMENT '存储路径',
  `file_size` BIGINT COMMENT '文件大小（字节）',
  `file_type` VARCHAR(32) COMMENT '文件类型（image/video/pdf）',
  `uploader_id` BIGINT COMMENT '上传人',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

-- 审核历史表
CREATE TABLE `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `action` VARCHAR(20) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) COMMENT '操作人姓名',
  `note` TEXT COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核历史表';

-- ==================== 初始化数据 ====================

-- 初始角色
INSERT INTO `sys_role` (`name`, `role_type`) VALUES ('总部管理员', 'admin'), ('企业管理员', 'enterprise');

-- 初始管理员账号 (密码: admin123, BCrypt加密)
-- 注意: deploy.sh 部署时会用随机生成的 ADMIN_PASSWORD 覆盖此默认密码
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `user_type`, `role_id`, `status`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin', 1, 1);

-- 初始溯源模板
INSERT INTO `trace_template` (`template_key`, `template_name`, `template_type`, `config_json`, `status`) VALUES
('default', '通用模板', '通用', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"},{"field":"enterprise.introduction","label":"企业简介","type":"text"},{"field":"enterprise.address_full","label":"企业地址"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.introduction","label":"产品介绍","type":"text"},{"field":"goods.storageMethod","label":"储存方式"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.testOrg","label":"检测机构"},{"field":"batch.testResult","label":"检测结果","type":"badge"}]},{"key":"cert","title":"认证信息","icon":"Medal","fields":[{"field":"cert.certName","label":"证书名称"},{"field":"cert.certTypeName","label":"证书类型"}]}],"theme":{"primaryColor":"#409eff","headerStyle":"gradient"}}', 1),
('vegetable', '蔬菜类模板', '蔬菜', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"},{"field":"enterprise.introduction","label":"企业简介","type":"text"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.sampleImage","label":"产品图片","type":"image"},{"field":"goods.introduction","label":"产品介绍","type":"text"},{"field":"goods.storageMethod","label":"储存方式"},{"field":"goods.eatingMethod","label":"食用方式"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.goodsSpec","label":"规格"},{"field":"batch.testCode","label":"检测编号"},{"field":"batch.testOrg","label":"检测机构"},{"field":"batch.testResult","label":"检测结果","type":"badge"},{"field":"batch.testReport","label":"检测报告","type":"file"}]},{"key":"base","title":"基地信息","icon":"Location","fields":[{"field":"base.name","label":"基地名称"},{"field":"base.planImage","label":"平面图","type":"image"},{"field":"base.realImage","label":"实景图","type":"image"},{"field":"base.areaDisplay","label":"面积"},{"field":"base.envReport","label":"环境检测报告","type":"file"}]}],"theme":{"primaryColor":"#22c55e","headerStyle":"gradient"}}', 1),
('fruit', '水果类模板', '水果', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.sampleImage","label":"产品图片","type":"image"},{"field":"goods.introduction","label":"产品介绍","type":"text"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.testResult","label":"检测结果","type":"badge"}]}],"theme":{"primaryColor":"#f59e0b","headerStyle":"gradient"}}', 1),
('meat', '肉禽类模板', '肉禽', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.introduction","label":"产品介绍","type":"text"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.testResult","label":"检测结果","type":"badge"}]}],"theme":{"primaryColor":"#ef4444","headerStyle":"gradient"}}', 1),
('grain', '粮油类模板', '粮油', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.introduction","label":"产品介绍","type":"text"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.testResult","label":"检测结果","type":"badge"}]}],"theme":{"primaryColor":"#d97706","headerStyle":"gradient"}}', 1),
('aquatic', '水产类模板', '水产', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.introduction","label":"产品介绍","type":"text"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.testResult","label":"检测结果","type":"badge"}]}],"theme":{"primaryColor":"#0ea5e9","headerStyle":"gradient"}}', 1);

-- 初始证书类型
INSERT INTO `cert_type` (`name`) VALUES ('有机认证'), ('绿色食品认证'), ('无公害认证'), ('地理标志认证'), ('ISO认证');

-- 初始产品
INSERT INTO `product` (`name`, `description`) VALUES
('蔬菜', '各类蔬菜产品'), ('水果', '各类水果产品'), ('肉禽', '肉禽类产品'), ('粮油', '粮油类产品'), ('水产', '水产类产品'), ('茶叶', '茶叶类产品');
