#!/bin/bash
echo "=== Order Flow Test ==="

# Login enterprise
LOGIN_RESP=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"test_enterprise","password":"test123","loginType":"enterprise"}')
ENT_TOKEN=$(echo $LOGIN_RESP | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("token",""))')
EH="Authorization: Bearer $ENT_TOKEN"

# Login admin
ADMIN_RESP=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","loginType":"admin"}')
ADMIN_TOKEN=$(echo $ADMIN_RESP | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("token",""))')
AH="Authorization: Bearer $ADMIN_TOKEN"

echo "=== Step 1: Submit existing order 6 ==="
SUB=$(curl -s -X POST "http://127.0.0.1:8080/api/enterprise/orders/6/submit" -H "$EH")
echo "submit_order6: $(echo $SUB | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:100])")"

echo ""
echo "=== Step 2: Check order status ==="
mysql -uroot -proot trace_system -e "SELECT id,order_no,status,submit_time FROM t_order WHERE enterprise_id=11;" 2>/dev/null

echo ""
echo "=== Step 3: Create new order with certId and address ==="
# Get cert for enterprise 11
CERT_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM enterprise_cert WHERE enterprise_id=11 LIMIT 1;" 2>/dev/null)
ADDR_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM address WHERE enterprise_id=11 ORDER BY id DESC LIMIT 1;" 2>/dev/null)
GOODS_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM goods WHERE enterprise_id=11 ORDER BY id DESC LIMIT 1;" 2>/dev/null)
BATCH_ID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM batch WHERE enterprise_id=11 ORDER BY id DESC LIMIT 1;" 2>/dev/null)
echo "cert_id=$CERT_ID addr_id=$ADDR_ID goods_id=$GOODS_ID batch_id=$BATCH_ID"

# Create order with cert
NEW_ORDER=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/orders \
  -H "$EH" -H 'Content-Type: application/json' \
  -d "{\"certId\":$CERT_ID,\"addressId\":$ADDR_ID}")
echo "create_order: $(echo $NEW_ORDER | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"

NEW_OID=$(mysql -uroot -proot trace_system -N -e "SELECT id FROM t_order WHERE enterprise_id=11 ORDER BY id DESC LIMIT 1;" 2>/dev/null)
echo "new_order_id=$NEW_OID"

echo ""
echo "=== Step 4: Add order items ==="
if [ -n "$NEW_OID" ]; then
  OI=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/order-items \
    -H "$EH" -H 'Content-Type: application/json' \
    -d "{\"orderId\":$NEW_OID,\"batchId\":$BATCH_ID,\"goodsId\":$GOODS_ID,\"goodsName\":\"有机大米\",\"goodsSpec\":\"5kg\",\"goodsWeight\":\"5kg\",\"quantity\":200,\"price\":25.5}")
  echo "add_item: $(echo $OI | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"
  
  OI2=$(curl -s -X POST http://127.0.0.1:8080/api/enterprise/order-items \
    -H "$EH" -H 'Content-Type: application/json' \
    -d "{\"orderId\":$NEW_OID,\"goodsId\":$GOODS_ID,\"goodsName\":\"有机大米(礼盒)\",\"goodsSpec\":\"2kg\",\"goodsWeight\":\"2kg\",\"quantity\":500,\"price\":45.0}")
  echo "add_item2: $(echo $OI2 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:80])")"
fi

echo ""
echo "=== Step 5: Submit new order ==="
if [ -n "$NEW_OID" ]; then
  SUB2=$(curl -s -X POST "http://127.0.0.1:8080/api/enterprise/orders/$NEW_OID/submit" -H "$EH")
  echo "submit: $(echo $SUB2 | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:100])")"
  
  STATUS=$(mysql -uroot -proot trace_system -N -e "SELECT status FROM t_order WHERE id=$NEW_OID;" 2>/dev/null)
  echo "status_after_submit=$STATUS"
fi

echo ""
echo "=== Step 6: Admin approve (if PENDING) ==="
if [ "$STATUS" = "PENDING" ] && [ -n "$NEW_OID" ]; then
  APP=$(curl -s -X POST "http://127.0.0.1:8080/api/admin/orders/$NEW_OID/approve" \
    -H "$AH" -H 'Content-Type: application/json' \
    -d '{"note":"审核通过，符合要求"}')
  echo "admin_approve: $(echo $APP | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'msg=',str(d.get('msg',''))[:100])")"
  
  STATUS2=$(mysql -uroot -proot trace_system -N -e "SELECT status FROM t_order WHERE id=$NEW_OID;" 2>/dev/null)
  echo "status_after_approve=$STATUS2"
fi

echo ""
echo "=== Step 7: Get order detail ==="
if [ -n "$NEW_OID" ]; then
  DETAIL=$(curl -s "http://127.0.0.1:8080/api/enterprise/orders/$NEW_OID" -H "$EH")
  echo "$DETAIL" | python3 -c "import sys,json; d=json.load(sys.stdin); data=d.get('data',{}); print('order_status=',data.get('order',{}).get('status','?'),'items=',len(data.get('orderItems',[])),'codes=',len(data.get('orderCodes',[])))"
fi

echo ""
echo "=== Step 8: Audit history ==="
if [ -n "$NEW_OID" ]; then
  AUDIT=$(curl -s "http://127.0.0.1:8080/api/enterprise/orders/$NEW_OID/audit-history" -H "$EH")
  echo "audit: $(echo $AUDIT | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'logs=',len(d.get('data',[])))")"
fi

echo ""
echo "=== Step 9: Admin order-codes for approved order ==="
if [ "$STATUS2" = "APPROVED" ] && [ -n "$NEW_OID" ]; then
  # Check admin order-codes endpoint
  CODES=$(curl -s "http://127.0.0.1:8080/api/admin/order-codes?orderId=$NEW_OID" -H "$AH")
  echo "admin_order_codes: $(echo $CODES | python3 -c "import sys,json; d=json.load(sys.stdin); print('code=',d.get('code'),'total=',d.get('data',{}).get('total','?'))")"
fi

echo ""
echo "=== Step 10: Error check ==="
journalctl -u trace-backend --no-pager -n 60 2>/dev/null | grep -iE 'ERROR' | grep -v 'at com\.\|at org\.\|at java\.' | tail -10

echo ""
echo "=== ALL DONE ==="
