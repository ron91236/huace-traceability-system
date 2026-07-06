-- V12 种子数据：视频监控 + IoT 物联网演示数据
-- 为企业ID=7（aliceshi）创建演示数据
-- ============================================================

-- 视频源（3个：工厂车间、种植基地、直播间）
INSERT INTO `video_source` (`enterprise_id`, `base_id`, `batch_id`, `camera_name`, `stream_url`, `stream_type`, `cover_image`, `platform`, `status`, `sort_order`) VALUES
(7, NULL, NULL, '工厂车间监控', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 'HLS', NULL, 'manual', 1, 1),
(7, 1, NULL, '种植基地实况', 'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8', 'HLS', NULL, 'manual', 1, 2),
(7, NULL, NULL, '直播间画面', 'https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8', 'HLS', NULL, 'manual', 1, 3);

-- IoT 设备（5个）
INSERT INTO `iot_device` (`enterprise_id`, `base_id`, `batch_id`, `device_name`, `device_type`, `product_key`, `device_key`, `location_desc`, `longitude`, `latitude`, `status`, `last_online_at`) VALUES
(7, 1, NULL, '土壤传感器-A01', 'soil_sensor', 'PK_DEMO_001', 'soil_sensor_01', '基地1号田东侧', 104.0657000, 30.6595000, 1, NOW()),
(7, 1, NULL, '土壤传感器-A02', 'soil_sensor', 'PK_DEMO_001', 'soil_sensor_02', '基地1号田西侧', 104.0648000, 30.6590000, 1, NOW()),
(7, 1, NULL, '温湿度传感器-B01', 'temp_sensor', 'PK_DEMO_002', 'temp_sensor_01', '基地温室大棚', 104.0660000, 30.6600000, 1, NOW()),
(7, NULL, 1, 'GPS追踪器-川A12345', 'gps_tracker', 'PK_DEMO_003', 'gps_tracker_01', '运输车辆', 104.0657000, 30.6595000, 1, NOW()),
(7, NULL, 1, '冷链温度计-C01', 'cold_chain', 'PK_DEMO_004', 'cold_chain_01', '冷链车厢', 104.0657000, 30.6595000, 1, NOW());

-- 告警规则（2条）
INSERT INTO `iot_alert_rule` (`enterprise_id`, `device_id`, `metric_name`, `operator`, `threshold`, `alert_level`, `alert_message`, `status`) VALUES
(7, NULL, 'temperature', '>', 35.00, 'WARNING', '温度超过35°C，请注意检查', 1),
(7, NULL, 'temperature', '<', -10.00, 'CRITICAL', '冷链温度异常，高于-10°C，请立即检查', 1);
