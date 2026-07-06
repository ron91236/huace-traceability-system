#!/bin/bash
# =========================================
# Git Hook 自动部署配置脚本 - trace-system
# 在阿里云服务器上执行此脚本
# =========================================

set -e

echo "=============================="
echo "  Git Hook 自动部署配置"
echo "  系统: trace-system"
echo "=============================="

# ---------- 配置变量 ----------
BARE_REPO_DIR="/opt/git-repos/trace-system.git"
DEPLOY_DIR="/opt/trace-system"
WEB_USER="root"  # 根据实际情况修改，如 www-data、nginx 等

# ---------- 1. 创建 bare Git 仓库 ----------
echo "[1/4] 创建 bare Git 仓库..."
mkdir -p "$BARE_REPO_DIR"
cd "$BARE_REPO_DIR"
git init --bare
echo "Bare 仓库创建完成: $BARE_REPO_DIR"

# ---------- 2. 创建 post-receive hook ----------
echo "[2/4] 配置 post-receive hook..."
cat > hooks/post-receive << 'HOOK'
#!/bin/bash
# Git post-receive hook for auto-deployment

while read oldrev newrev refname; do
    branch=$(echo "$refname" | sed 's|refs/heads/||')
    
    if [ "$branch" = "master" ]; then
        echo "========================================"
        echo "检测到 master 分支推送，开始自动部署..."
        echo "========================================"
        
        # 设置环境变量
        export GIT_WORK_TREE=/opt/trace-system
        export GIT_DIR=/opt/git-repos/trace-system.git
        
        # 拉取最新代码
        echo "[1/6] 拉取最新代码..."
        git checkout -f master
        
        # 构建前端
        echo "[2/6] 构建前端..."
        cd /opt/trace-system/trace-frontend
        npm install --legacy-peer-deps 2>/dev/null || npm install
        npm run build 2>/dev/null || npx vite build
        echo "前端构建完成"
        
        # 构建后端
        echo "[3/6] 构建后端..."
        cd /opt/trace-system/trace-backend
        mvn clean package -DskipTests -q 2>/dev/null || mvn clean package -DskipTests
        echo "后端构建完成"
        
        # 重启后端服务
        echo "[4/6] 重启后端服务..."
        systemctl restart trace-backend
        sleep 5
        
        # 重启 Nginx
        echo "[5/6] 重启 Nginx..."
        systemctl restart nginx
        
        # 检查服务状态
        echo "[6/6] 检查服务状态..."
        if curl -s http://127.0.0.1:8080/api/auth/login >/dev/null 2>&1; then
            echo "✅ 后端服务运行正常"
        else
            echo "❌ 后端服务启动失败，请检查日志"
            journalctl -u trace-backend -n 50 --no-pager
        fi
        
        echo "========================================"
        echo "部署完成！"
        echo "========================================"
    fi
done
HOOK

chmod +x hooks/post-receive
echo "post-receive hook 配置完成"

# ---------- 3. 确保部署目录存在 ----------
echo "[3/4] 检查部署目录..."
if [ ! -d "$DEPLOY_DIR" ]; then
    echo "部署目录不存在，正在克隆代码..."
    git clone "$BARE_REPO_DIR" "$DEPLOY_DIR"
else
    echo "部署目录已存在: $DEPLOY_DIR"
fi

# ---------- 4. 配置权限 ----------
echo "[4/4] 配置权限..."
chown -R "$WEB_USER:$WEB_USER" "$BARE_REPO_DIR"
chown -R "$WEB_USER:$WEB_USER" "$DEPLOY_DIR"

echo ""
echo "=============================="
echo "  ✅ Git Hook 配置完成！"
echo "=============================="
echo ""
echo "下一步操作："
echo "1. 在本地添加远程仓库："
echo "   cd /path/to/trace-system"
echo "   git remote add deploy root@8.152.162.118:/opt/git-repos/trace-system.git"
echo ""
echo "2. 推送到服务器触发部署："
echo "   git push deploy master"
echo ""
echo "=============================="
