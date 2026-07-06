#!/bin/bash
echo "=== Check token structure ==="
LOGIN_RESP=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"test_enterprise","password":"test123","loginType":"enterprise"}')
echo "$LOGIN_RESP" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(json.dumps(d.get("data",{}), indent=2, ensure_ascii=False))'

echo ""
echo "=== Check DB data ==="
mysql -uroot -proot trace_system 2>/dev/null <<'EOF'
SELECT 'test_report' as tbl, COUNT(*) as cnt FROM test_report WHERE enterprise_id=11
UNION ALL SELECT 'goods', COUNT(*) FROM goods WHERE enterprise_id=11
UNION ALL SELECT 'batch', COUNT(*) FROM batch WHERE enterprise_id=11
UNION ALL SELECT 'address', COUNT(*) FROM address WHERE enterprise_id=11
UNION ALL SELECT 't_order', COUNT(*) FROM t_order WHERE enterprise_id=11
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item oi JOIN t_order o ON oi.order_id=o.id WHERE o.enterprise_id=11;
EOF

echo ""
echo "=== Recent test_report records ==="
mysql -uroot -proot trace_system -e "SELECT id,enterprise_id,report_name,test_code FROM test_report ORDER BY id DESC LIMIT 5;" 2>/dev/null

echo ""
echo "=== Recent goods records ==="
mysql -uroot -proot trace_system -e "SELECT id,enterprise_id,name FROM goods ORDER BY id DESC LIMIT 5;" 2>/dev/null

echo ""
echo "=== Recent batch records ==="
mysql -uroot -proot trace_system -e "SELECT id,enterprise_id,name FROM batch ORDER BY id DESC LIMIT 5;" 2>/dev/null

echo ""
echo "=== Recent address records ==="
mysql -uroot -proot trace_system -e "SELECT id,enterprise_id,contact FROM address ORDER BY id DESC LIMIT 5;" 2>/dev/null

echo ""
echo "=== Recent order records ==="
mysql -uroot -proot trace_system -e "SELECT id,enterprise_id,order_no,status FROM t_order ORDER BY id DESC LIMIT 5;" 2>/dev/null

echo "DONE"
