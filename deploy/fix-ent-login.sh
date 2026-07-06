#!/bin/bash
BASE=http://127.0.0.1:8080/api

echo "=== ENTERPRISE TABLE STRUCTURE ==="
mysql -uroot -proot trace_system -N -e "SHOW COLUMNS FROM enterprise;" 2>/dev/null | head -20

echo "=== ENTERPRISE LOGIN DATA ==="
mysql -uroot -proot trace_system -N -e "SELECT id,name,login_account,status FROM enterprise LIMIT 10;" 2>/dev/null

echo "=== SET PASSWORD FOR FIRST ENTERPRISE ==="
# Use Python to generate hash and update DB
python3 << 'PYEOF'
import subprocess
try:
    from passlib.hash import bcrypt
    h = bcrypt.hash('test123')
    print(f"HASH={h}")
    # Get first enterprise ID
    r = subprocess.run(['mysql','-uroot','-proot','trace_system','-N','-e','SELECT id FROM enterprise ORDER BY id LIMIT 1;'], capture_output=True, text=True)
    eid = r.stdout.strip()
    print(f"FIRST_ENT_ID={eid}")
    if eid:
        subprocess.run(['mysql','-uroot','-proot','trace_system','-e',
            f"UPDATE enterprise SET login_account='test_enterprise', login_password_hash='{h}', status=1 WHERE id={eid};"], capture_output=True, text=True)
        print(f"Updated enterprise {eid}: login_account=test_enterprise, password=test123")
except Exception as e:
    print(f"ERROR: {e}")
PYEOF

echo "=== TEST ENTERPRISE LOGIN ==="
ENT_RESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"username":"test_enterprise","password":"test123","loginType":"enterprise"}')
echo "LOGIN_RESP: $(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg",""))' 2>/dev/null)"

ENT_TOKEN=$(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("token",""))' 2>/dev/null)
echo "ENT_TOKEN_LEN=${#ENT_TOKEN}"

if [ ${#ENT_TOKEN} -gt 10 ]; then
  EH="Authorization: Bearer $ENT_TOKEN"

  echo "=== ENTERPRISE API TESTS ==="
  for ep in "test-reports" "test-reports/all" "batches" "certs" "bases" "goods" "addresses" "orders" "notices" "dashboard/stats" "order-codes" "code-usages" "data-screen/all"; do
    RESP=$(curl -s "$BASE/enterprise/$ep" -H "$EH")
    CODE=$(echo $RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("code","?"))' 2>/dev/null)
    MSG=$(echo $RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("msg","")[:60])' 2>/dev/null)
    echo "enterprise/$ep: code=$CODE msg=$MSG"
  done

  echo "=== CRUD TESTS ==="
  # Create test report
  CR=$(curl -s -X POST "$BASE/enterprise/test-reports" -H "$EH" -H 'Content-Type: application/json' -d '{"reportName":"API测试报告","testCode":"TEST-001","testOrg":"测试机构","testTime":"2026-01-01","testMethod":"标准方法","testBasis":"GB/T 12345","testType":"型式检验","testResult":"合格"}')
  echo "create_test_report: $(echo $CR | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  REPORT_ID=$(echo $CR | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

  # Update test report
  if [ -n "$REPORT_ID" ]; then
    UR=$(curl -s -X PUT "$BASE/enterprise/test-reports/$REPORT_ID" -H "$EH" -H 'Content-Type: application/json' -d '{"reportName":"API测试报告-已更新","testCode":"TEST-001","testOrg":"测试机构更新","testTime":"2026-02-01","testMethod":"标准方法V2","testBasis":"GB/T 12345","testType":"出厂检验","testResult":"合格"}')
    echo "update_test_report: $(echo $UR | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  fi

  # Create batch
  CB=$(curl -s -X POST "$BASE/enterprise/batches" -H "$EH" -H 'Content-Type: application/json' -d '{"name":"测试批次-001","goodsSpec":"500g/瓶"}')
  echo "create_batch: $(echo $CB | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  BATCH_ID=$(echo $CB | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

  # Update batch
  if [ -n "$BATCH_ID" ]; then
    UB=$(curl -s -X PUT "$BASE/enterprise/batches/$BATCH_ID" -H "$EH" -H 'Content-Type: application/json' -d '{"name":"测试批次-已更新","goodsSpec":"1000g/瓶"}')
    echo "update_batch: $(echo $UB | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  fi

  # Create goods
  CG=$(curl -s -X POST "$BASE/enterprise/goods" -H "$EH" -H 'Content-Type: application/json' -d '{"name":"测试商品","spec":"250g","unit":"瓶"}')
  echo "create_goods: $(echo $CG | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  GOODS_ID=$(echo $CG | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

  # Delete goods
  if [ -n "$GOODS_ID" ]; then
    DG=$(curl -s -X DELETE "$BASE/enterprise/goods/$GOODS_ID" -H "$EH")
    echo "delete_goods: $(echo $DG | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  fi

  # Create address
  CA=$(curl -s -X POST "$BASE/enterprise/addresses" -H "$EH" -H 'Content-Type: application/json' -d '{"contactName":"张三","phone":"13900139000","province":"广东省","city":"深圳市","district":"南山区","address":"测试路1号","isDefault":0}')
  echo "create_address: $(echo $CA | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  ADDR_ID=$(echo $CA | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

  # Delete address
  if [ -n "$ADDR_ID" ]; then
    DA=$(curl -s -X DELETE "$BASE/enterprise/addresses/$ADDR_ID" -H "$EH")
    echo "delete_address: $(echo $DA | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  fi

  # Delete test report
  if [ -n "$REPORT_ID" ]; then
    DR=$(curl -s -X DELETE "$BASE/enterprise/test-reports/$REPORT_ID" -H "$EH")
    echo "delete_test_report: $(echo $DR | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  fi

  # === ORDER FLOW TEST ===
  echo "=== ORDER FLOW ==="
  CO=$(curl -s -X POST "$BASE/enterprise/orders" -H "$EH" -H 'Content-Type: application/json' -d '{"certTypeId":1,"productId":1,"quantity":100,"remark":"测试订单"}')
  echo "create_order: $(echo $CO | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  ORDER_ID=$(echo $CO | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

  if [ -n "$ORDER_ID" ]; then
    SO=$(curl -s -X POST "$BASE/enterprise/orders/$ORDER_ID/submit" -H "$EH")
    echo "submit_order: $(echo $SO | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"

    # Delete order
    DO=$(curl -s -X DELETE "$BASE/enterprise/orders/$ORDER_ID" -H "$EH")
    echo "delete_order: $(echo $DO | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:60])' 2>/dev/null)"
  fi
fi

echo "=== RECENT ERRORS ==="
journalctl -u trace-backend --no-pager -n 50 2>/dev/null | grep -iE 'error|exception' | tail -10

echo "ALLEND"
