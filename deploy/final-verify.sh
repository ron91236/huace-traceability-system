#!/bin/bash
echo "=== Final data fix and verification ==="

mysql -uroot -proot trace_system 2>/dev/null <<'SQL'
-- Add trace_inventory with correct fields
INSERT INTO trace_inventory (code_pool, label_spec_id, start_serial, end_serial, produce_time, enterprise_id)
SELECT 'POOL-001', 1, '00000001', '00000100', '2026-06-15 10:00:00', 11
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM trace_inventory WHERE enterprise_id=11 AND code_pool='POOL-001');

INSERT INTO trace_inventory (code_pool, label_spec_id, start_serial, end_serial, produce_time, enterprise_id)
SELECT 'POOL-002', 2, '00000101', '00000200', '2026-06-20 14:00:00', 11
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM trace_inventory WHERE enterprise_id=11 AND code_pool='POOL-002');

INSERT INTO trace_inventory (code_pool, label_spec_id, start_serial, end_serial, produce_time, enterprise_id)
SELECT 'POOL-FARM-001', 1, '00001001', '00001100', '2026-07-01 09:00:00', 12
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM trace_inventory WHERE enterprise_id=12 AND code_pool='POOL-FARM-001');

-- Check if test_report for enterprise 12 was inserted, if not add them
INSERT INTO test_report (enterprise_id, report_name, test_code, test_org, test_time, test_method, test_basis, test_type, test_result)
VALUES (12, '蔬菜农残检测', 'RPT-2026-NC-001', '省农产品质量安全检测中心', '2026-05-15 10:00:00', 'NY/T 761-2008', 'GB 2763-2021', '委托检验', '合格')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO test_report (enterprise_id, report_name, test_code, test_org, test_time, test_method, test_basis, test_type, test_result)
VALUES (12, '土壤重金属检测', 'RPT-2026-HM-001', '环境监测站', '2026-03-20 14:30:00', 'GB/T 17141', 'GB 15618-2018', '型式检验', '合格')
ON DUPLICATE KEY UPDATE id=id;

SQL

echo ""
echo "=== Final data counts ==="
mysql -uroot -proot trace_system 2>/dev/null <<'SQL'
SELECT 'enterprise' as tbl, COUNT(*) as cnt FROM enterprise
UNION ALL SELECT 'enterprise_cert', COUNT(*) FROM enterprise_cert
UNION ALL SELECT 'enterprise_base', COUNT(*) FROM enterprise_base
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'cert_type', COUNT(*) FROM cert_type
UNION ALL SELECT 'goods', COUNT(*) FROM goods
UNION ALL SELECT 'batch', COUNT(*) FROM batch
UNION ALL SELECT 'test_report', COUNT(*) FROM test_report
UNION ALL SELECT 'address', COUNT(*) FROM address
UNION ALL SELECT 't_order', COUNT(*) FROM t_order
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'order_code', COUNT(*) FROM order_code
UNION ALL SELECT 'notice', COUNT(*) FROM notice
UNION ALL SELECT 'label_spec', COUNT(*) FROM label_spec
UNION ALL SELECT 'trace_inventory', COUNT(*) FROM trace_inventory
UNION ALL SELECT 'code_package', COUNT(*) FROM code_package
UNION ALL SELECT 'scan_record', COUNT(*) FROM scan_record;
SQL

echo ""
echo "=== Comprehensive API test (enterprise) ==="
LOGIN=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"test_enterprise","password":"test123","loginType":"enterprise"}')
TK=$(echo $LOGIN | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("token",""))')
EH="Authorization: Bearer $TK"

for ep in test-reports test-reports/all batches certs bases goods addresses orders notices dashboard/stats order-codes code-usages data-screen/all group/children; do
  CODE=$(curl -s "http://127.0.0.1:8080/api/enterprise/$ep" -H "$EH" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code','ERR'))" 2>/dev/null)
  echo "  enterprise/$ep: $CODE"
done

echo ""
echo "=== Comprehensive API test (admin) ==="
ALOGIN=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123","loginType":"admin"}')
ATK=$(echo $ALOGIN | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("token",""))')
AH="Authorization: Bearer $ATK"

for ep in cert-types enterprises enterprises/all products label-specs bases orders notices dashboard/stats code-packages code-packages/all code-packages/last-serial data-screen/all trace-templates voided-code-ranges; do
  CODE=$(curl -s "http://127.0.0.1:8080/api/admin/$ep" -H "$AH" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code','ERR'))" 2>/dev/null)
  echo "  admin/$ep: $CODE"
done

echo ""
echo "=== Test trace/query (public) ==="
TQ=$(curl -s "http://127.0.0.1:8080/api/trace/query?serialNo=00000001")
echo "trace/query: $(echo $TQ | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:60])" 2>/dev/null)"

echo ""
echo "=== Test all enterprise logins ==="
for acct in aliceshi mu zi admin test_enterprise farm_001; do
  CODE=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$acct\",\"password\":\"test123\",\"loginType\":\"enterprise\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code','ERR'))" 2>/dev/null)
  echo "  login $acct: $CODE"
done

echo ""
echo "ALL_DONE"
