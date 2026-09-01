#!/bin/bash
echo "=== Check frontend and nginx ==="

echo "=== 1. Nginx status ==="
systemctl status nginx --no-pager 2>/dev/null | head -5

echo ""
echo "=== 2. Nginx config ==="
cat /etc/nginx/conf.d/trace.conf 2>/dev/null || cat /etc/nginx/nginx.conf 2>/dev/null | head -40

echo ""
echo "=== 3. Frontend dist ==="
ls -la /opt/trace-system/trace-frontend/dist/ 2>/dev/null | head -10
ls -la /opt/trace-system/trace-frontend/dist/assets/ 2>/dev/null | head -10

echo ""
echo "=== 4. Test frontend pages ==="
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/
echo " - /"
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/login
echo " - /login"
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/enterprise/test-reports
echo " - /enterprise/test-reports"
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/enterprise/batches
echo " - /enterprise/batches"

echo ""
echo "=== 5. Test API through nginx ==="
# 公开接口健康检查（不依赖凭据）
curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1/api/trace/00000626"
echo " - /api/trace/00000626 (公开接口)"

# Login and test enterprise API through nginx
TOKEN=$(curl -s -X POST http://127.0.0.1/api/auth/login -H 'Content-Type: application/json' -d '{"username":"test_enterprise","password":"test123","loginType":"enterprise"}' | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("token",""))')
EH="Authorization: Bearer $TOKEN"

curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1/api/enterprise/test-reports" -H "$EH"
echo " - /api/enterprise/test-reports"
curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1/api/enterprise/test-reports/all" -H "$EH"
echo " - /api/enterprise/test-reports/all"
curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1/api/enterprise/batches" -H "$EH"
echo " - /api/enterprise/batches"

echo ""
echo "=== 6. Backend logs check ==="
journalctl -u trace-backend --no-pager -n 30 2>/dev/null | grep -iE 'ERROR' | grep -v 'at com\.\|at org\.\|at java\.' | tail -5

echo ""
echo "DONE"
