#!/bin/bash
# 在服务器上执行: ADMIN_PASS=xxx bash /tmp/test-apis.sh
ADMIN_PASS="${ADMIN_PASS:-}"
if [ -z "$ADMIN_PASS" ]; then
  echo "请设置 ADMIN_PASS 环境变量后执行"
  exit 1
fi

echo "=== 1. 检查后端服务状态 ==="
systemctl is-active trace-backend

echo ""
echo "=== 2. 测试登录API ==="
LOGIN_RESP=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"$ADMIN_PASS\",\"loginType\":\"enterprise\"}")
echo "$LOGIN_RESP" | python3 -m json.tool 2>/dev/null || echo "$LOGIN_RESP"

# 提取token
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null)
echo ""
echo "Token: ${TOKEN:0:50}..."

if [ -z "$TOKEN" ]; then
  echo "ERROR: 无法获取token，尝试备用方式提取"
  TOKEN=$(echo "$LOGIN_RESP" | grep -oP '"token"\s*:\s*"\K[^"]+')
  echo "Token(备用): ${TOKEN:0:50}..."
fi

if [ -z "$TOKEN" ]; then
  echo "ERROR: 仍无法获取token，终止测试"
  exit 1
fi

echo ""
echo "=== 3. 测试检测报告API ==="
curl -s "http://127.0.0.1:8080/api/enterprise/test-reports" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>&1 | head -40

echo ""
echo "=== 4. 测试批次管理API ==="
curl -s "http://127.0.0.1:8080/api/enterprise/batches" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>&1 | head -40

echo ""
echo "=== 5. 测试商品管理API ==="
curl -s "http://127.0.0.1:8080/api/enterprise/goods" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>&1 | head -40

echo ""
echo "=== 6. 测试基地管理API ==="
curl -s "http://127.0.0.1:8080/api/enterprise/bases" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>&1 | head -40

echo ""
echo "=== 7. 测试订单管理API ==="
curl -s "http://127.0.0.1:8080/api/enterprise/orders" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>&1 | head -40

echo ""
echo "=== 8. 测试Dashboard统计API ==="
curl -s "http://127.0.0.1:8080/api/enterprise/dashboard/stats" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>&1 | head -20

echo ""
echo "=== 9. 测试Nginx代理 ==="
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost/api/enterprise/test-reports" \
  -H "Authorization: Bearer $TOKEN")
echo "Nginx代理 HTTP 状态码: $HTTP_CODE"

echo ""
echo "=== API测试完成 ==="
