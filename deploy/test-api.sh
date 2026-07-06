#!/bin/bash
BASE=http://127.0.0.1:8080/api

# Admin login
ADMIN_TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123","loginType":"admin"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["token"])' 2>/dev/null)
echo "ADMIN_TOKEN_LEN=${#ADMIN_TOKEN}"

# DB users
echo "--- SYS_USER ---"
mysql -uroot -proot trace_system -N -e 'SELECT id,username,user_type,enterprise_id FROM sys_user;' 2>/dev/null
echo "--- ENTERPRISE ---"
mysql -uroot -proot trace_system -N -e 'SELECT id,name,contact_name,phone FROM enterprise LIMIT 5;' 2>/dev/null

# Enterprise login - try first enterprise contact phone
PHONE=$(mysql -uroot -proot trace_system -N -e 'SELECT phone FROM enterprise WHERE id=1;' 2>/dev/null)
CONTACT=$(mysql -uroot -proot trace_system -N -e 'SELECT contact_name FROM enterprise WHERE id=1;' 2>/dev/null)
ENT_NAME=$(mysql -uroot -proot trace_system -N -e 'SELECT name FROM enterprise WHERE id=1;' 2>/dev/null)
echo "ENT1: phone=$PHONE contact=$CONTACT name=$ENT_NAME"

# Try login with phone
ENT_RESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$PHONE\",\"password\":\"123456\",\"loginType\":\"enterprise\"}")
echo "ENT_LOGIN_PHONE: $(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg",""))' 2>/dev/null)"

ENT_TOKEN=$(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("token",""))' 2>/dev/null)
if [ ${#ENT_TOKEN} -lt 10 ]; then
  ENT_RESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$CONTACT\",\"password\":\"123456\",\"loginType\":\"enterprise\"}")
  echo "ENT_LOGIN_CONTACT: $(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg",""))' 2>/dev/null)"
  ENT_TOKEN=$(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("token",""))' 2>/dev/null)
fi
if [ ${#ENT_TOKEN} -lt 10 ]; then
  # Try with enterprise name
  ENT_RESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ENT_NAME\",\"password\":\"123456\",\"loginType\":\"enterprise\"}")
  echo "ENT_LOGIN_NAME: $(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg",""))' 2>/dev/null)"
  ENT_TOKEN=$(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("token",""))' 2>/dev/null)
fi
if [ ${#ENT_TOKEN} -lt 10 ]; then
  echo "--- CHECK USER PASSWORDS ---"
  mysql -uroot -proot trace_system -N -e "SELECT id,username,user_type,password FROM sys_user WHERE user_type!='admin' LIMIT 5;" 2>/dev/null
  # Try admin123
  ENT_RESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$PHONE\",\"password\":\"admin123\",\"loginType\":\"enterprise\"}")
  echo "ENT_LOGIN_ADMIN123: $(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg",""))' 2>/dev/null)"
  ENT_TOKEN=$(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("token",""))' 2>/dev/null)
fi
if [ ${#ENT_TOKEN} -lt 10 ]; then
  # Try username from sys_user
  ENT_USER=$(mysql -uroot -proot trace_system -N -e "SELECT username FROM sys_user WHERE user_type!='admin' LIMIT 1;" 2>/dev/null)
  echo "TRYING_SYS_USER=$ENT_USER"
  ENT_RESP=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ENT_USER\",\"password\":\"123456\",\"loginType\":\"enterprise\"}")
  echo "ENT_LOGIN_SYS: $(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg",""))' 2>/dev/null)"
  ENT_TOKEN=$(echo $ENT_RESP | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("token",""))' 2>/dev/null)
fi
echo "ENT_TOKEN_LEN=${#ENT_TOKEN}"

AH="Authorization: Bearer $ADMIN_TOKEN"
EH="Authorization: Bearer $ENT_TOKEN"

api() {
  local label=$1 url=$2 token=$3
  local resp=$(curl -s "$BASE$url" -H "$token")
  echo "$label: $(echo $resp | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"msg=",d.get("msg","")[:80])' 2>/dev/null)"
}

api_data() {
  local label=$1 url=$2 token=$3
  local resp=$(curl -s "$BASE$url" -H "$token")
  echo "$label: $(echo $resp | python3 -c '
import sys,json
d=json.load(sys.stdin)
data=d.get("data",{})
if isinstance(data,dict):
    t=data.get("total","N/A")
elif isinstance(data,list):
    t=len(data)
else:
    t="?"
print("code=",d.get("code"),"total=",t)
' 2>/dev/null)"
}

echo "=== ADMIN API ==="
api_data "admin/enterprises" "/admin/enterprises" "$AH"
api_data "admin/products" "/admin/products" "$AH"
api_data "admin/cert-types" "/admin/cert-types" "$AH"
api_data "admin/notices" "/admin/notices" "$AH"
api_data "admin/label-specs" "/admin/label-specs" "$AH"
api "admin/dashboard" "/admin/dashboard/stats" "$AH"

echo "=== ENTERPRISE API ==="
api_data "ent/test-reports" "/enterprise/test-reports" "$EH"
api "ent/test-reports/all" "/enterprise/test-reports/all" "$EH"
api_data "ent/batches" "/enterprise/batches" "$EH"
api_data "ent/certs" "/enterprise/certs" "$EH"
api_data "ent/bases" "/enterprise/bases" "$EH"
api_data "ent/goods" "/enterprise/goods" "$EH"
api_data "ent/addresses" "/enterprise/addresses" "$EH"
api_data "ent/orders" "/enterprise/orders" "$EH"
api_data "ent/notices" "/enterprise/notices" "$EH"
api "ent/dashboard" "/enterprise/dashboard/stats" "$EH"
api "ent/order-codes" "/enterprise/order-codes" "$EH"
api "ent/code-usages" "/enterprise/code-usages" "$EH"
api "ent/data-screen" "/enterprise/data-screen/all" "$EH"

echo "=== TRACE API ==="
api "trace/templates" "/trace/templates" ""
api "trace_query" "/trace/query?code=TEST001" ""

echo "=== CRUD TESTS ==="
# Create test report
CR=$(curl -s -X POST "$BASE/enterprise/test-reports" -H "$EH" -H 'Content-Type: application/json' -d '{"reportName":"API测试报告","testCode":"TEST-001","testOrg":"测试机构","testTime":"2026-01-01","testMethod":"标准方法","testBasis":"GB/T 12345","testType":"型式检验","testResult":"合格"}')
echo "create_test_report: $(echo $CR | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"))' 2>/dev/null)"
REPORT_ID=$(echo $CR | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

# Update test report
if [ -n "$REPORT_ID" ]; then
  UR=$(curl -s -X PUT "$BASE/enterprise/test-reports/$REPORT_ID" -H "$EH" -H 'Content-Type: application/json' -d '{"reportName":"API测试报告-已更新","testCode":"TEST-001-UPD","testOrg":"测试机构更新","testTime":"2026-02-01","testMethod":"标准方法V2","testBasis":"GB/T 12345-2026","testType":"出厂检验","testResult":"合格"}')
  echo "update_test_report: $(echo $UR | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"))' 2>/dev/null)"
fi

# Create batch
CB=$(curl -s -X POST "$BASE/enterprise/batches" -H "$EH" -H 'Content-Type: application/json' -d '{"name":"测试批次-001","goodsSpec":"500g/瓶"}')
echo "create_batch: $(echo $CB | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"))' 2>/dev/null)"
BATCH_ID=$(echo $CB | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

# Update batch
if [ -n "$BATCH_ID" ]; then
  UB=$(curl -s -X PUT "$BASE/enterprise/batches/$BATCH_ID" -H "$EH" -H 'Content-Type: application/json' -d '{"name":"测试批次-001-已更新","goodsSpec":"1000g/瓶"}')
  echo "update_batch: $(echo $UB | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"))' 2>/dev/null)"
fi

# Create goods
CG=$(curl -s -X POST "$BASE/enterprise/goods" -H "$EH" -H 'Content-Type: application/json' -d '{"name":"测试商品","spec":"250g","unit":"瓶","categoryId":1}')
echo "create_goods: $(echo $CG | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"))' 2>/dev/null)"
GOODS_ID=$(echo $CG | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

# Delete goods
if [ -n "$GOODS_ID" ]; then
  DG=$(curl -s -X DELETE "$BASE/enterprise/goods/$GOODS_ID" -H "$EH")
  echo "delete_goods: $(echo $DG | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"))' 2>/dev/null)"
fi

# Create address
CA=$(curl -s -X POST "$BASE/enterprise/addresses" -H "$EH" -H 'Content-Type: application/json' -d '{"contactName":"张三","phone":"13900139000","province":"广东省","city":"深圳市","district":"南山区","address":"测试路1号","isDefault":0}')
echo "create_address: $(echo $CA | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"),"id=",d.get("data",{}).get("id","?"))' 2>/dev/null)"
ADDR_ID=$(echo $CA | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("id",""))' 2>/dev/null)

# Delete address
if [ -n "$ADDR_ID" ]; then
  DA=$(curl -s -X DELETE "$BASE/enterprise/addresses/$ADDR_ID" -H "$EH")
  echo "delete_address: $(echo $DA | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"))' 2>/dev/null)"
fi

# Delete test report
if [ -n "$REPORT_ID" ]; then
  DR=$(curl -s -X DELETE "$BASE/enterprise/test-reports/$REPORT_ID" -H "$EH")
  echo "delete_test_report: $(echo $DR | python3 -c 'import sys,json; d=json.load(sys.stdin); print("code=",d.get("code"))' 2>/dev/null)"
fi

echo "=== RECENT ERRORS ==="
journalctl -u trace-backend --no-pager -n 50 2>/dev/null | grep -iE 'error|exception' | tail -5

echo "DONE"
