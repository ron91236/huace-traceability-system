#!/bin/bash
echo "=== Add supplementary test data ==="

mysql -uroot -proot trace_system 2>/dev/null <<'SQL'

-- 1. Add certs for enterprise 12 (绿色生态农场)
INSERT INTO enterprise_cert (cert_type_id, enterprise_id, cert_name, product_name, start_date, end_date, is_void)
SELECT 2, 12, '有机产品认证', '有机蔬菜', '2025-06-01', '2028-06-01', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM enterprise_cert WHERE enterprise_id=12 AND cert_name='有机产品认证');

INSERT INTO enterprise_cert (cert_type_id, enterprise_id, cert_name, product_name, start_date, end_date, is_void)
SELECT 3, 12, '绿色食品认证', '有机生菜', '2025-09-01', '2027-09-01', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM enterprise_cert WHERE enterprise_id=12 AND cert_name='绿色食品认证');

-- 2. Add more goods for enterprise 12
INSERT INTO goods (product_id, enterprise_id, name, package_spec, weight_spec, introduction, storage_method)
SELECT 2, 12, '有机西红柿', '1kg/盒', '1kg', '绿色有机种植', '冷藏保存'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE enterprise_id=12 AND name='有机西红柿');

INSERT INTO goods (product_id, enterprise_id, name, package_spec, weight_spec, introduction, storage_method)
SELECT 3, 12, '有机黄瓜', '500g/袋', '500g', '无农药种植', '阴凉干燥处'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE enterprise_id=12 AND name='有机黄瓜');

-- 3. Add more batches for enterprise 12
INSERT INTO batch (name, goods_id, goods_spec, enterprise_id, test_code, test_org, test_time, test_method, test_basis, test_type, test_result)
SELECT '2026年夏季生菜批次', g.id, '1kg/盒', 12, 'RPT-2026-LS-001', '市农产品检测中心', '2026-06-01 09:00:00', 'GB/T 5009.12', 'GB 2762-2022', '型式检验', '合格'
FROM goods g WHERE g.enterprise_id=12 AND g.name='有机生菜'
AND NOT EXISTS (SELECT 1 FROM batch WHERE enterprise_id=12 AND name='2026年夏季生菜批次');

-- 4. Add order items to orders 5 and 6 (which have no cert, but let's add items)
-- Order 5 is PENDING for enterprise 10
INSERT INTO order_item (order_id, goods_id, goods_name, goods_spec, goods_weight, quantity, price)
SELECT 5, g.id, g.name, g.package_spec, g.weight_spec, 300, 15.00
FROM goods g WHERE g.enterprise_id=10 LIMIT 1
AND NOT EXISTS (SELECT 1 FROM order_item WHERE order_id=5);

-- Order 6 is PENDING for enterprise 11
INSERT INTO order_item (order_id, goods_id, goods_name, goods_spec, goods_weight, quantity, price)
SELECT 6, g.id, g.name, g.package_spec, g.weight_spec, 150, 32.00
FROM goods g WHERE g.enterprise_id=11 LIMIT 1
AND NOT EXISTS (SELECT 1 FROM order_item WHERE order_id=6);

-- 5. Add trace_inventory records (条码使用记录)
INSERT INTO trace_inventory (enterprise_id, code_package_id, code_package_item_id, goods_id, batch_id, cert_id, usage_time, remark)
SELECT cp.enterprise_id, cp.id, cpi.id, g.id, b.id, ec.id, NOW(), '自动测试记录'
FROM code_package cp
JOIN code_package_item cpi ON cpi.package_id = cp.id AND cpi.bind_status = 'BOUND'
LEFT JOIN enterprise_cert ec ON ec.enterprise_id = cp.enterprise_id
LEFT JOIN goods g ON g.enterprise_id = cp.enterprise_id
LEFT JOIN batch b ON b.enterprise_id = cp.enterprise_id
WHERE NOT EXISTS (SELECT 1 FROM trace_inventory WHERE code_package_item_id = cpi.id)
LIMIT 5;

-- 6. Add more test reports for enterprise 12
INSERT INTO test_report (enterprise_id, report_name, test_code, test_org, test_time, test_method, test_basis, test_type, test_result)
SELECT 12, '蔬菜农残检测', 'RPT-2026-NC-001', '省农产品质量安全检测中心', '2026-05-15 10:00:00', 'NY/T 761-2008', 'GB 2763-2021', '委托检验', '合格'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM test_report WHERE enterprise_id=12 AND test_code='RPT-2026-NC-001');

INSERT INTO test_report (enterprise_id, report_name, test_code, test_org, test_time, test_method, test_basis, test_type, test_result)
SELECT 12, '土壤重金属检测', 'RPT-2026-HM-001', '环境监测站', '2026-03-20 14:30:00', 'GB/T 17141', 'GB 15618-2018', '型式检验', '合格'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM test_report WHERE enterprise_id=12 AND test_code='RPT-2026-HM-001');

-- 7. Add address for enterprise 12
INSERT INTO address (enterprise_id, contact, phone, address, zipcode)
SELECT 12, '王大伯', '13800138000', '浙江省杭州市余杭区良渚街道农场路168号', '311113'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM address WHERE enterprise_id=12);

-- 8. Create orders for enterprise 12 with cert
INSERT INTO t_order (order_no, enterprise_id, cert_id, status, address_id, submit_time)
SELECT CONCAT('ORD20260704EC', id), 12, ec.id, 'APPROVED', a.id, '2026-07-01 10:00:00'
FROM enterprise_cert ec
LEFT JOIN address a ON a.enterprise_id = 12
WHERE ec.enterprise_id = 12 AND ec.cert_name = '有机产品认证'
AND NOT EXISTS (SELECT 1 FROM t_order WHERE enterprise_id=12 AND cert_id=ec.id)
LIMIT 1;

SQL

echo "Data inserted"

echo ""
echo "=== Verify ==="
mysql -uroot -proot trace_system 2>/dev/null <<'SQL'
SELECT 'enterprise_cert' as tbl, COUNT(*) as cnt FROM enterprise_cert
UNION ALL SELECT 'goods', COUNT(*) FROM goods
UNION ALL SELECT 'batch', COUNT(*) FROM batch
UNION ALL SELECT 'test_report', COUNT(*) FROM test_report
UNION ALL SELECT 'address', COUNT(*) FROM address
UNION ALL SELECT 't_order', COUNT(*) FROM t_order
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'trace_inventory', COUNT(*) FROM trace_inventory;
SQL

echo ""
echo "=== Test code-usages API ==="
LOGIN_RESP=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"test_enterprise","password":"test123","loginType":"enterprise"}')
ENT_TOKEN=$(echo $LOGIN_RESP | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("token",""))')
EH="Authorization: Bearer $ENT_TOKEN"

CU=$(curl -s "http://127.0.0.1:8080/api/enterprise/code-usages" -H "$EH")
echo "code-usages: $(echo $CU | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'total=',d.get('data',{}).get('total',0))")"

echo ""
echo "=== Create code-usage record ==="
CU_CREATE=$(curl -s -X POST "http://127.0.0.1:8080/api/enterprise/code-usages" \
  -H "$EH" -H 'Content-Type: application/json' \
  -d '{"remark":"生产线A使用"}')
echo "create_code_usage: $(echo $CU_CREATE | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

echo ""
echo "=== Delete order 7 (DRAFT test data cleanup) ==="
DEL=$(curl -s -X DELETE "http://127.0.0.1:8080/api/enterprise/orders/7" -H "$EH")
echo "delete_draft_order7: $(echo $DEL | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

echo ""
echo "=== Error check ==="
journalctl -u trace-backend --no-pager -n 30 2>/dev/null | grep -iE 'ERROR' | grep -v 'at com\.\|at org\.\|at java\.' | tail -5

echo ""
echo "DONE"
