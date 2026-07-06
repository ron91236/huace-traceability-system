#!/bin/bash
# =========================================
# Git Hook 自动部署脚本 - Docker Compose 版本
# =========================================

set -e

echo "========================================"
echo "  Docker Compose 自动部署开始"
echo "========================================"

DEPLOY_DIR="/opt/trace-system"
cd "$DEPLOY_DIR"

# 拉取最新代码
echo ""
echo "[1/6] 拉取最新代码..."
git checkout -f master
echo "✅ 代码拉取完成"

# 构建前端
echo ""
echo "[2/6] 构建前端..."
cd trace-frontend
npm install --legacy-peer-deps 2>/dev/null || npm install
npm run build 2>/dev/null || npx vite build
echo "✅ 前端构建完成"

# 停止旧容器
echo ""
echo "[3/6] 停止旧容器..."
cd ..
docker-compose down || true
echo "✅ 旧容器已停止"

# 重新构建并启动所有服务
echo ""
echo "[4/6] 构建并启动 Docker 服务（这可能需要几分钟）..."
docker-compose up -d --build
echo "✅ Docker 服务已启动"

# 等待后端启动
echo ""
echo "[5/6] 等待后端服务启动..."
for i in $(seq 1 60); do
    if curl -s http://localhost:8080/api/auth/login >/dev/null 2>&1; then
        echo "✅ 后端服务已就绪"
        break
    fi
    echo "   等待中... ($i/60)"
    sleep 2
done

# 检查服务状态
echo ""
echo "[6/6] 检查服务状态..."
docker-compose ps

echo ""
echo "========================================"
echo "  ✅ 部署完成！"
echo "========================================"
echo ""
echo "访问地址："
echo "  前端: http://$(curl -s ifconfig.me 2>/dev/null || echo '服务器IP')"
echo "  API:  http://$(curl -s ifconfig.me 2>/dev/null || echo '服务器IP'):8080"
echo ""
echo "查看日志："
echo "  docker-compose logs -f backend"
echo ""
echo "========================================"
