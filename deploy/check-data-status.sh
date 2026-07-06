#!/bin/bash
echo "=== Check current data status ==="
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
UNION ALL SELECT 'code_package_item', COUNT(*) FROM code_package_item
UNION ALL SELECT 'scan_record', COUNT(*) FROM scan_record;
SQL

echo ""
echo "=== Enterprise certs per enterprise ==="
mysql -uroot -proot trace_system -e "SELECT e.id,e.name,COUNT(ec.id) as certs FROM enterprise e LEFT JOIN enterprise_cert ec ON e.id=ec.enterprise_id GROUP BY e.id,e.name;" 2>/dev/null

echo ""
echo "=== Bases per enterprise ==="
mysql -uroot -proot trace_system -e "SELECT e.id,e.name,COUNT(eb.id) as bases FROM enterprise e LEFT JOIN enterprise_base eb ON e.id=eb.enterprise_id GROUP BY e.id,e.name;" 2>/dev/null

echo ""
echo "=== Orders and items ==="
mysql -uroot -proot trace_system -e "SELECT o.id,o.order_no,o.status,o.cert_id,COUNT(oi.id) as items FROM t_order o LEFT JOIN order_item oi ON o.id=oi.order_id GROUP BY o.id,o.order_no,o.status,o.cert_id;" 2>/dev/null

echo "DONE"
