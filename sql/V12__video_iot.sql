-- V12: 视频监控 + IoT 物联网模块
-- ============================================================

-- 视频源配置表
CREATE TABLE IF NOT EXISTS `video_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `base_id` BIGINT DEFAULT NULL COMMENT '关联基地(可选)',
  `batch_id` BIGINT DEFAULT NULL COMMENT '关联批次(可选)',
  `camera_name` VARCHAR(128) NOT NULL COMMENT '摄像头名称',
  `stream_url` VARCHAR(512) COMMENT '直播流地址(HLS/FLV)',
  `stream_type` VARCHAR(20) DEFAULT 'HLS' COMMENT 'HLS/FLV/RTMP',
  `cover_image` VARCHAR(255) COMMENT '封面图',
  `platform` VARCHAR(32) DEFAULT 'manual' COMMENT '来源平台: aliyun/tencent/manual',
  `device_id` VARCHAR(128) COMMENT '云平台设备ID',
  `status` TINYINT DEFAULT 1 COMMENT '1=启用,0=禁用',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_video_enterprise` (`enterprise_id`),
  KEY `idx_video_base` (`base_id`),
  KEY `idx_video_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频源配置表';

-- IoT 设备注册表
CREATE TABLE IF NOT EXISTS `iot_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `base_id` BIGINT DEFAULT NULL COMMENT '关联基地',
  `batch_id` BIGINT DEFAULT NULL COMMENT '关联批次',
  `device_name` VARCHAR(128) NOT NULL COMMENT '设备名称',
  `device_type` VARCHAR(32) NOT NULL COMMENT '设备类型: soil_sensor/temp_sensor/humidity_sensor/ph_sensor/gps_tracker/cold_chain',
  `product_key` VARCHAR(128) COMMENT '阿里云IoT ProductKey',
  `device_key` VARCHAR(128) COMMENT '阿里云IoT DeviceName',
  `iot_instance_id` VARCHAR(128) COMMENT '阿里云IoT实例ID',
  `location_desc` VARCHAR(255) COMMENT '安装位置描述',
  `longitude` DECIMAL(10,7) COMMENT '安装经度',
  `latitude` DECIMAL(10,7) COMMENT '安装纬度',
  `status` TINYINT DEFAULT 1 COMMENT '1=在线,0=离线,2=告警',
  `last_online_at` DATETIME COMMENT '最后在线时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_iot_enterprise` (`enterprise_id`),
  KEY `idx_iot_base` (`base_id`),
  KEY `idx_iot_batch` (`batch_id`),
  KEY `idx_iot_device_type` (`device_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备注册表';

-- IoT 告警规则表
CREATE TABLE IF NOT EXISTS `iot_alert_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `device_id` BIGINT DEFAULT NULL COMMENT '指定设备(NULL=全局规则)',
  `metric_name` VARCHAR(64) NOT NULL COMMENT '指标名: temperature/humidity/ph/soil_moisture',
  `operator` VARCHAR(10) NOT NULL COMMENT '比较运算符: >, <, >=, <=, ==',
  `threshold` DECIMAL(10,2) NOT NULL COMMENT '阈值',
  `alert_level` VARCHAR(20) DEFAULT 'WARNING' COMMENT 'WARNING/CRITICAL',
  `alert_message` VARCHAR(255) COMMENT '告警消息模板',
  `status` TINYINT DEFAULT 1 COMMENT '1=启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_rule_enterprise` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT告警规则表';

-- IoT 告警记录表
CREATE TABLE IF NOT EXISTS `iot_alert_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL COMMENT '所属企业',
  `device_id` BIGINT NOT NULL COMMENT '触发设备',
  `rule_id` BIGINT COMMENT '触发规则',
  `metric_name` VARCHAR(64) NOT NULL COMMENT '指标名',
  `metric_value` DECIMAL(10,2) NOT NULL COMMENT '实际值',
  `threshold` DECIMAL(10,2) COMMENT '阈值',
  `alert_level` VARCHAR(20) COMMENT 'WARNING/CRITICAL',
  `alert_message` VARCHAR(512) COMMENT '告警消息',
  `handle_status` TINYINT DEFAULT 0 COMMENT '0=未处理,1=已处理',
  `handle_note` TEXT COMMENT '处理备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_record_enterprise` (`enterprise_id`),
  KEY `idx_alert_record_device` (`device_id`),
  KEY `idx_alert_record_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT告警记录表';
