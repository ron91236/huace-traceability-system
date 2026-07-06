-- ========================================
-- 产品溯源系统 - 测试数据初始化脚本
-- 基于实际数据库表结构编写
-- ========================================

USE trace_system;

-- 1. 插入测试企业（admin 用户已存在，这里添加其他测试企业）
-- 密码均为 admin123 的 BCrypt 哈希
INSERT IGNORE INTO enterprise (login_account, login_password_hash, name, nature, industry, contact, phone, account_type, status, credit_code, email, province, city, district, address, introduction) VALUES
('test_enterprise', '$2a$10$MKCpT2hj0K/1j4.8WKliXufYyCP/97.Wy0UURoJXiDUnjlZ9mVYja', '测试农业科技有限公司', '企业', '农业', '张三', '13900139001', '种植', 1, '91110000MA0012345X', 'test@example.com', '北京市', '海淀区', '中关村街道', '中关村大街1号', '专注于有机蔬菜种植与销售的现代化农业企业'),
('farm_001', '$2a$10$MKCpT2hj0K/1j4.8WKliXufYyCP/97.Wy0UURoJXiDUnjlZ9mVYja', '绿色生态农场', '农场', '种植', '李四', '13900139002', '种植', 1, '91110000MA0012346Y', 'farm@example.com', '山东省', '潍坊市', '寿光市', '蔬菜基地A区', '以绿色生态理念经营的大型农场');

-- 2. 插入测试基地
INSERT IGNORE INTO enterprise_base (enterprise_id, name, code, area, unit, manager, phone, province, city, district, certification) VALUES
(10, '华测检测认证集团-生产基地', 'BASE001', 500.00, '亩', '王经理', '13900139010', '山东省', '潍坊市', '寿光市', '有机认证'),
(11, '测试农业科技-种植基地', 'BASE002', 1000.00, '亩', '赵经理', '13900139011', '北京市', '海淀区', '中关村街道', '绿色食品认证'),
(12, '绿色生态农场-A区', 'BASE003', 300.00, '亩', '李经理', '13900139012', '山东省', '潍坊市', '寿光市', '无公害认证');

-- 3. 插入测试商品（关联 product 表的产品大类）
INSERT IGNORE INTO goods (product_id, enterprise_id, name, package_spec, weight_spec, introduction, storage_method, eating_method) VALUES
(1, 10, '有机西红柿', '500g/盒', '500g', '新鲜有机西红柿，无农药残留，口感酸甜', '冷藏保存，保质期7天', '生食、炒菜、做汤'),
(1, 10, '精品黄瓜', '1kg/袋', '1kg', '优质黄瓜，口感脆嫩，绿色无公害', '冷藏保存，保质期5天', '生食、凉拌、炒菜'),
(2, 11, '红富士苹果', '5kg/箱', '5kg', '山东红富士苹果，甜度高，口感好', '常温保存，保质期30天', '生食、榨汁'),
(1, 12, '有机生菜', '300g/包', '300g', '新鲜有机生菜，无农药残留', '冷藏保存，保质期3天', '生食、沙拉');

-- 4. 插入测试批次（关联商品和基地）
INSERT IGNORE INTO batch (enterprise_id, name, goods_id, goods_spec, base_id, test_org, test_result, test_type, test_method, test_basis) VALUES
(10, '2026年7月第1批西红柿', 1, '500g/盒', 1, '华测检测认证集团', '合格', '抽检', '实验室检测', 'GB/T 5009'),
(10, '2026年7月第1批黄瓜', 2, '1kg/袋', 1, '华测检测认证集团', '合格', '抽检', '实验室检测', 'GB/T 5009'),
(11, '2026年7月苹果批次', 3, '5kg/箱', 2, '第三方检测机构', '合格', '全检', '现场检测', 'NY/T 1431'),
(12, '2026年7月生菜批次', 4, '300g/包', 3, '华测检测认证集团', '合格', '抽检', '实验室检测', 'GB/T 5009');

-- 5. 插入测试检测报告
INSERT IGNORE INTO test_report (enterprise_id, report_name, test_org, test_result, test_type, test_method, test_basis) VALUES
(10, '西红柿农残检测报告', '华测检测认证集团', '合格', '抽检', '实验室检测', 'GB/T 5009'),
(10, '黄瓜质量检测报告', '华测检测认证集团', '合格', '抽检', '实验室检测', 'GB/T 5009'),
(11, '苹果糖度检测报告', '第三方检测机构', '合格', '全检', '现场检测', 'NY/T 1431');

-- 6. 插入收货地址
INSERT IGNORE INTO address (enterprise_id, contact, phone, address, zipcode) VALUES
(10, '张三', '13900139001', '北京市朝阳区建国路100号A座', '100020'),
(10, '李四', '13900139002', '上海市浦东新区陆家嘴环路1000号', '200120');

-- 7. 插入测试订单（状态: DRAFT/PENDING/APPROVED/REJECTED）
INSERT IGNORE INTO t_order (order_no, enterprise_id, status, address_id, submit_time) VALUES
('ORD20260704001', 10, 'APPROVED', 1, '2026-07-01 10:00:00'),
('ORD20260704002', 10, 'PENDING', 2, '2026-07-02 14:00:00');

-- 8. 插入订单明细
INSERT IGNORE INTO order_item (order_id, batch_id, goods_id, goods_name, goods_spec, quantity, price, total_price) VALUES
(1, 1, 1, '有机西红柿', '500g/盒', 100, 15.00, 1500.00),
(2, 2, 2, '精品黄瓜', '1kg/袋', 200, 12.00, 2400.00);

-- 9. 插入企业认证信息（关联 cert_type 表，1=有机认证, 2=绿色食品认证, 3=无公害认证, 4=地理标志认证, 5=ISO认证）
INSERT IGNORE INTO enterprise_cert (cert_type_id, enterprise_id, cert_name, product_name, start_date, end_date, is_void) VALUES
(1, 10, '有机产品认证证书', '有机西红柿', '2025-01-01', '2027-01-01', 0),
(5, 10, 'ISO9001质量管理体系认证', '全部产品', '2025-06-01', '2028-06-01', 0),
(2, 11, '绿色食品认证', '红富士苹果', '2025-03-01', '2027-03-01', 0);

-- 10. 插入公告
INSERT IGNORE INTO notice (title, content, enterprise_id) VALUES
('系统升级通知', '系统将于本周日进行维护升级，预计耗时2小时，请提前做好准备。', NULL),
('新功能上线', '检测报告管理功能已上线，欢迎使用！', NULL),
('测试企业专属通知', '这是一条发给测试企业的专属通知。', 10);

-- 11. 插入标签规格（用于订单条码绑定）
INSERT IGNORE INTO label_spec (spec_name, material, price, usage_method, support_manual_assign, is_void) VALUES
('标准溯源标签', '铜版纸', 0.15, '贴标', 1, 0),
('防伪吊牌标签', 'PET', 0.30, '挂牌', 0, 0),
('防水标签', '合成纸', 0.25, '贴标', 1, 0);

-- 验证导入结果
SELECT '=== 测试数据导入结果 ===' as info;
SELECT 'enterprise' as `table`, COUNT(*) as count FROM enterprise
UNION ALL SELECT 'enterprise_base', COUNT(*) FROM enterprise_base
UNION ALL SELECT 'goods', COUNT(*) FROM goods
UNION ALL SELECT 'batch', COUNT(*) FROM batch
UNION ALL SELECT 'test_report', COUNT(*) FROM test_report
UNION ALL SELECT 'address', COUNT(*) FROM address
UNION ALL SELECT 't_order', COUNT(*) FROM t_order
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'enterprise_cert', COUNT(*) FROM enterprise_cert
UNION ALL SELECT 'notice', COUNT(*) FROM notice
UNION ALL SELECT 'label_spec', COUNT(*) FROM label_spec;
