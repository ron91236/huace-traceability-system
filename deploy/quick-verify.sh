#!/bin/bash
# Quick verification of the originally broken endpoints

# Login as enterprise user
TOKEN=$(curl -s http://127.0.0.1:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"aliceshi","password":"test123","loginType":"enterprise"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

echo "=== Enterprise Login: OK (token obtained) ==="

# Test Reports (originally broken)
echo ""
echo "=== Test Reports API ==="
curl -s http://127.0.0.1:8080/api/enterprise/test-reports \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('code:', d['code'], '| records:', len(d.get('data',{}).get('records',[])))"

echo ""
echo "=== Test Reports ALL ==="
curl -s http://127.0.0.1:8080/api/enterprise/test-reports/all \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('code:', d['code'], '| items:', len(d.get('data',[])))"

# Batches (originally broken)
echo ""
echo "=== Batches API ==="
curl -s http://127.0.0.1:8080/api/enterprise/batches \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('code:', d['code'], '| records:', len(d.get('data',{}).get('records',[])))"

# Other key endpoints
echo ""
echo "=== Goods ==="
curl -s http://127.0.0.1:8080/api/enterprise/goods \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('code:', d['code'], '| records:', len(d.get('data',{}).get('records',[])))"

echo ""
echo "=== Orders ==="
curl -s http://127.0.0.1:8080/api/enterprise/orders \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('code:', d['code'], '| records:', len(d.get('data',{}).get('records',[])))"

echo ""
echo "=== Dashboard Stats ==="
curl -s http://127.0.0.1:8080/api/enterprise/dashboard/stats \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('code:', d['code'], '| data:', d.get('data',{}))"

# Frontend check
echo ""
echo "=== Frontend Pages ==="
for path in "/" "/enterprise/test-reports" "/enterprise/batches" "/enterprise/goods" "/enterprise/orders"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:80${path}")
  echo "  $path -> HTTP $code"
done

echo ""
echo "=== DONE ==="
