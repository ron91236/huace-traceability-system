-- V13: VR全景虚拟导览

CREATE TABLE IF NOT EXISTS `vr_scene` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `base_id` BIGINT COMMENT '关联基地',
  `name` VARCHAR(128) COMMENT '场景名称(如：生产车间A)',
  `panorama_url` VARCHAR(512) NOT NULL COMMENT '全景图URL',
  `hfov` INT DEFAULT 120 COMMENT '水平视角(度)',
  `vfov` INT DEFAULT 90 COMMENT '垂直视角(度)',
  `sort_order` INT DEFAULT 0 COMMENT '排序(导览顺序)',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否为入口场景',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enterprise` (`enterprise_id`),
  KEY `idx_base` (`base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VR全景场景表';

CREATE TABLE IF NOT EXISTS `vr_hotspot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `scene_id` BIGINT NOT NULL COMMENT '所属场景',
  `type` VARCHAR(20) DEFAULT 'scene' COMMENT 'scene=跳转场景, info=信息标注',
  `target_scene_id` BIGINT COMMENT '目标场景ID(跳转用)',
  `label` VARCHAR(128) COMMENT '热点标签文字',
  `tooltip` TEXT COMMENT '信息提示(支持HTML)',
  `h_yaw` DECIMAL(8,4) COMMENT '水平偏航角(度)',
  `v_pitch` DECIMAL(8,4) COMMENT '垂直俯仰角(度)',
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VR热点表';
