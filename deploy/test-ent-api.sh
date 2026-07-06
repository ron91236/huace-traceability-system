#!/bin/bash
# Enterprise API full test script - corrected field names

echo "=== Step 1: Generate bcrypt hash ==="
HASH=$(python3 -c 'from passlib.hash import bcrypt; print(bcrypt.hash("test123"))')
echo "HASH generated OK"

echo "=== Step 2: Update all enterprise passwords ==="
mysql -uroot -proot trace_system -e "UPDATE enterprise SET login_password_hash='$HASH', status=1;" 2>/dev/null
echo "Passwords updated"

echo "=== Step 3: Test enterprise login ==="
LOGIN_RESP=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"test_enterprise","password":"test123","loginType":"enterprise"}')
LOGIN_CODE=$(echo $LOGIN_RESP | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code","?"))')
echo "Login: code=$LOGIN_CODE"

ENT_TOKEN=$(echo $LOGIN_RESP | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("token",""))')
echo "ENT_TOKEN_LEN=${#ENT_TOKEN}"

if [ ${#ENT_TOKEN} -eq 0 ]; then
  echo "FATAL: No token"
  exit 1
fi

EH="Authorization: Bearer $ENT_TOKEN"
ENT_ID=$(echo $LOGIN_RESP | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("enterpriseId",""))')
echo "Enterprise ID from token: $ENT_ID"

echo ""
echo "=== Step 4: All GET endpoints ==="
for ep in test-reports batches certs bases goods addresses orders notices dashboard/stats order-codes code-usages; do
  CODE=$(curl -s "http://127.0.0.1:8080/api/enterprise/$ep" -H "$EH" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code','?'))" 2>/dev/null)
  echo "  $ep: code=$CODE"
done
for ep in test-reports/all data-screen/all group/children; do
  CODE=$(curl -s "http://127.0.0.1:8080/api/enterprise/$ep" -H "$EH" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code','?'))" 2>/dev/null)
  echo "  $ep: code=$CODE"
done

echo ""
echo "=== Step 5: CRUD - Test Reports ==="
# Create (testTime must be yyyy-MM-dd HH:mm:ss)
R1=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/test-reports \
  -H "$EH" -H 'Content-Type: application/json' \
  -d '{"reportName":"API测试报告","testCode":"RPT-2026-001","testOrg":"国家质检中心","testTime":"2026-01-15 10:30:00","testMethod":"GB/T 5009.12","testBasis":"GB 2762-2022","testType":"型式检验","testResult":"合格"}')
echo "create_report1: $(echo $R1 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

R2=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/test-reports \
  -H "$EH" -H 'Content-Type: application/json' \
  -d '{"reportName":"农残检测报告","testCode":"RPT-2026-002","testOrg":"省农业检测所","testTime":"2026-02-20 14:00:00","testMethod":"NY/T 761-2008","testBasis":"GB 2763-2021","testType":"委托检验","testResult":"合格"}')
echo "create_report2: $(echo $R2 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

# Get IDs from DB
REPORT_IDS=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM test_report WHERE enterprise_id=$ENT_ID ORDER BY id DESC LIMIT 2;" 2>/dev/null)
REPORT_ID1=$(echo "$REPORT_IDS" | tail -1)
REPORT_ID2=$(echo "$REPORT_IDS" | head -1)
echo "report_ids: $REPORT_ID1, $REPORT_ID2"

# Update
if [ -n "$REPORT_ID1" ]; then
  UPD=$(curl -s -X PUT "http://127.0.0.1:8080/api/enterprise/test-reports/$REPORT_ID1" \
    -H "$EH" -H 'Content-Type: application/json' \
    -d '{"reportName":"API测试报告-已更新","testResult":"优秀"}')
  echo "update_report: $(echo $UPD | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'))")"
fi

# Delete second
if [ -n "$REPORT_ID2" ]; then
  DEL=$(curl -s -X DELETE "http://127.0.0.1:8080/api/enterprise/test-reports/$REPORT_ID2" -H "$EH")
  echo "delete_report: $(echo $DEL | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'))")"
fi

echo ""
echo "=== Step 6: CRUD - Goods ==="
G1=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/goods \
  -H "$EH" -H 'Content-Type: application/json' \
  -d '{"name":"有机大米","packageSpec":"5kg/袋","weightSpec":"5kg","introduction":"东北有机大米","storageMethod":"阴凉干燥处","eatingMethod":"蒸煮均可"}')
echo "create_goods: $(echo $G1 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

GOODS_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM goods WHERE enterprise_id=$ENT_ID ORDER BY id DESC LIMIT 1;" 2>/dev/null)
echo "goods_id=$GOODS_ID"

echo ""
echo "=== Step 7: CRUD - Batches ==="
BATCH_BODY="{\"name\":\"2026年春季批次\",\"goodsSpec\":\"5kg\"}"
if [ -n "$GOODS_ID" ]; then BATCH_BODY="{\"name\":\"2026年春季批次\",\"goodsSpec\":\"5kg\",\"goodsId\":$GOODS_ID}"; fi
if [ -n "$REPORT_ID1" ]; then BATCH_BODY=$(echo $BATCH_BODY | sed "s/}$/,\"testReportId\":$REPORT_ID1}/"); fi

B1=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/batches \
  -H "$EH" -H 'Content-Type: application/json' \
  -d "$BATCH_BODY")
echo "create_batch: $(echo $B1 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

BATCH_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM batch WHERE enterprise_id=$ENT_ID ORDER BY id DESC LIMIT 1;" 2>/dev/null)
echo "batch_id=$BATCH_ID"

echo ""
echo "=== Step 8: CRUD - Addresses ==="
A1=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/addresses \
  -H "$EH" -H 'Content-Type: application/json' \
  -d '{"contact":"张三","phone":"13900139000","address":"广东省深圳市南山区科技园路88号","zipcode":"518000"}')
echo "create_address: $(echo $A1 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

ADDR_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM address WHERE enterprise_id=$ENT_ID ORDER BY id DESC LIMIT 1;" 2>/dev/null)
echo "address_id=$ADDR_ID"

# Update address
if [ -n "$ADDR_ID" ]; then
  UPD=$(curl -s -X PUT "http://127.0.0.1:8080/api/enterprise/addresses/$ADDR_ID" \
    -H "$EH" -H 'Content-Type: application/json' \
    -d '{"contact":"张三(更新)","phone":"13900139001","address":"广东省深圳市南山区科技园路99号"}')
  echo "update_address: $(echo $UPD | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'))")"
fi

echo ""
echo "=== Step 9: CRUD - Orders ==="
# Get a certId for this enterprise
CERT_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM enterprise_cert WHERE enterprise_id=$ENT_ID LIMIT 1;" 2>/dev/null)
echo "cert_id=$CERT_ID"

ORDER_BODY='{"status":"DRAFT"}'
if [ -n "$CERT_ID" ]; then ORDER_BODY="{\"certId\":$CERT_ID}"; fi

O1=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/orders \
  -H "$EH" -H 'Content-Type: application/json' \
  -d "$ORDER_BODY")
echo "create_order: $(echo $O1 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

ORDER_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM t_order WHERE enterprise_id=$ENT_ID ORDER BY id DESC LIMIT 1;" 2>/dev/null)
echo "order_id=$ORDER_ID"

# Add order item
if [ -n "$ORDER_ID" ]; then
  ITEM_BODY="{\"orderId\":$ORDER_ID"
  if [ -n "$BATCH_ID" ]; then ITEM_BODY="$ITEM_BODY,\"batchId\":$BATCH_ID"; fi
  if [ -n "$GOODS_ID" ]; then ITEM_BODY="$ITEM_BODY,\"goodsId\":$GOODS_ID"; fi
  ITEM_BODY="$ITEM_BODY,\"goodsName\":\"有机大米\",\"goodsSpec\":\"5kg\",\"goodsWeight\":\"5kg\",\"quantity\":100,\"price\":25.5}"
  
  OI1=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/order-items \
    -H "$EH" -H 'Content-Type: application/json' \
    -d "$ITEM_BODY")
  echo "add_order_item: $(echo $OI1 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"
  
  # Set address on order
  if [ -n "$ADDR_ID" ]; then
    UPD=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/orders \
      -H "$EH" -H 'Content-Type: application/json' \
      -d "{\"certId\":$CERT_ID,\"addressId\":$ADDR_ID}")
    echo "create_order_with_addr: $(echo $UPD | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'))")"
  fi
  
  # Submit order
  SUB=$(curl -s -X POST "http://127.0.0.1:8080/api/enterprise/orders/$ORDER_ID/submit" -H "$EH")
  echo "submit_order: $(echo $SUB | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"
  
  # Check order status
  ORD_STATUS=$(mysql -uroot -proot trace_system -N -e "SELECT status FROM t_order WHERE id=$ORDER_ID;" 2>/dev/null)
  echo "order_status_after_submit=$ORD_STATUS"
  
  # Get order detail
  DETAIL=$(curl -s "http://127.0.0.1:8080/api/enterprise/orders/$ORDER_ID" -H "$EH")
  echo "order_detail: $(echo $DETAIL | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'))")"
fi

echo ""
echo "=== Step 10: List all data ==="
for tbl in test_report goods batch address t_order order_item; do
  CNT=$(mysql -uroot -proot trace_system -N -e "SELECT COUNT(*) FROM $tbl WHERE enterprise_id=$ENT_ID;" 2>/dev/null)
  echo "  $tbl: $CNT records"
done

echo ""
echo "=== Step 11: Error Logs ==="
journalctl -u trace-backend --no-pager -n 80 2>/dev/null | grep -iE 'ERROR|Exception' | grep -v 'at com\.' | tail -15

echo ""
echo "=== DONE ==="
