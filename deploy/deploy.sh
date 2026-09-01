#!/bin/bash
# =========================================
# 产品溯源系统 - 阿里云一键部署脚本
# Alibaba Cloud Linux 3 / CentOS 8+
# =========================================
set -e

echo "=============================="
echo "  产品溯源系统部署开始"
echo "=============================="

# ---------- 1. 安装基础依赖 ----------
echo "[1/6] 安装基础依赖..."
yum install -y java-17-openjdk java-17-openjdk-devel nginx mysql-server curl unzip 2>/dev/null || true

# ---------- 2. 安装 Node.js 18 ----------
echo "[2/6] 安装 Node.js..."
if ! command -v node &>/dev/null || [ "$(node -v | cut -d. -f1 | tr -d v)" -lt 18 ]; then
    curl -fsSL https://rpm.nodesource.com/setup_18.x | bash - 2>/dev/null || true
    yum install -y nodejs 2>/dev/null || true
fi
echo "Node.js: $(node -v 2>/dev/null || echo 'not found')"
echo "npm: $(npm -v 2>/dev/null || echo 'not found')"

# ---------- 3. 配置 MySQL / Redis / MongoDB ----------
echo "[3/6] 配置数据库..."
systemctl start mysqld 2>/dev/null || systemctl start mysql 2>/dev/null || true
systemctl enable mysqld 2>/dev/null || systemctl enable mysql 2>/dev/null || true

# 生成随机数据库密码与 JWT 密钥（不写入仓库；可用同名环境变量覆盖）
DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -base64 18 | tr -dc 'A-Za-z0-9' | head -c 20)}"
JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c 48)}"
echo "已生成数据库密码与JWT密钥，请妥善保存到本地密码管理器中"

# Redis
if ! command -v redis-server &>/dev/null && ! systemctl list-unit-files | grep -q redis; then
    yum install -y redis 2>/dev/null || true
fi
# 允许远程访问
sed -i 's/^bind 127.0.0.1/bind 0.0.0.0/' /etc/redis.conf 2>/dev/null || true
sed -i 's/^protected-mode yes/protected-mode no/' /etc/redis.conf 2>/dev/null || true
systemctl start redis 2>/dev/null || true
systemctl enable redis 2>/dev/null || true

# MongoDB
if ! command -v mongod &>/dev/null; then
    echo "安装 MongoDB..."
    cat > /etc/yum.repos.d/mongodb-org-6.0.repo << 'MONGOREPO'
[mongodb-org-6.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/$releasever/mongodb-org/6.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-6.0.asc
MONGOREPO
    yum install -y mongodb-org 2>/dev/null || true
fi
# 允许远程访问
sed -i 's/bindIp: 127.0.0.1/bindIp: 0.0.0.0/' /etc/mongod.conf 2>/dev/null || true
systemctl start mongod 2>/dev/null || true
systemctl enable mongod 2>/dev/null || true

# 设置 root 密码并创建数据库
mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$DB_PASSWORD';" 2>/dev/null || true
mysql -u root -p"$DB_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS trace_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
# 出于安全考虑不再创建 root@'%' 远程账号，本地开发请通过 SSH 隧道连接数据库
mysql -u root -p"$DB_PASSWORD" -e "FLUSH PRIVILEGES;" 2>/dev/null || true

# 导入建表脚本
if [ -f /opt/trace-system/sql/V1__init_schema.sql ]; then
    mysql -u root -p"$DB_PASSWORD" < /opt/trace-system/sql/V1__init_schema.sql 2>/dev/null || true
    echo "数据库初始化完成"
fi

# 生成随机管理员密码（可用同名环境变量覆盖），覆盖种子数据中的默认密码
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
while [ -z "$ADMIN_PASSWORD" ] || [ ${#ADMIN_PASSWORD} -lt 16 ]; do
    ADMIN_PASSWORD="$(openssl rand -base64 16 | tr -dc 'A-Za-z0-9')"
done
if ! command -v htpasswd &>/dev/null; then
    yum install -y httpd-tools 2>/dev/null || true
fi
ADMIN_HASH=$(htpasswd -bnBC 10 "" "$ADMIN_PASSWORD" | tr -d ':\n')
mysql -u root -p"$DB_PASSWORD" -e "UPDATE trace_system.sys_user SET password_hash='$ADMIN_HASH' WHERE username='admin';" 2>/dev/null || true
echo "管理员密码已生成，请妥善保存到本地密码管理器中"

# ---------- 4. 构建前端 ----------
echo "[4/6] 构建前端..."
cd /opt/trace-system/trace-frontend
npm install --legacy-peer-deps 2>/dev/null || npm install
npm run build 2>/dev/null || npx vite build
echo "前端构建完成"

# ---------- 5. 构建后端 ----------
echo "[5/6] 构建后端..."
cd /opt/trace-system/trace-backend

# 安装 Maven（如果没有）
if ! command -v mvn &>/dev/null; then
    yum install -y maven 2>/dev/null || true
fi

# 修改 application.yml 使用服务器 MySQL
cat > src/main/resources/application-prod.yml << 'YAML'
server:
  port: 8080
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css
    min-response-size: 1024

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/trace_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: 2000ms
    mongodb:
      uri: mongodb://localhost:27017/trace_system
      auto-index-creation: true
  cache:
    type: redis
    redis:
      time-to-live: 86400000
      cache-null-values: true

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

jwt:
  secret: ${JWT_SECRET:}
  expiration: 86400000

file:
  upload-dir: /data/trace/uploads
  url-prefix: /uploads

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
YAML

mvn clean package -DskipTests -q 2>/dev/null || mvn clean package -DskipTests
echo "后端构建完成"

# ---------- 6. 配置并启动服务 ----------
echo "[6/6] 配置服务..."

# 创建上传目录
mkdir -p /data/trace/uploads

# 配置 Nginx
cat > /etc/nginx/conf.d/trace.conf << 'NGINX'
server {
    listen 80;
    server_name _;

    root /opt/trace-system/trace-frontend/dist;
    index index.html;

    # 前端路由 - Vue Router history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 50m;
    }

    # 上传文件访问
    location ^~ /uploads/ {
        alias /data/trace/uploads/;
    }

    # 海报公开页面
    location ^~ /poster/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # 禁止缓存海报页面（防止微信浏览器缓存404等错误页面）
        add_header Cache-Control "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0" always;
        add_header Pragma "no-cache" always;
        add_header Expires "0" always;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml;
    gzip_min_length 1024;
}
NGINX

# 停止默认 Nginx 站点
rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true

# 启动 Nginx
systemctl restart nginx
systemctl enable nginx
echo "Nginx 已启动"

# 创建后端 systemd 服务
cat > /etc/systemd/system/trace-backend.service << SVC
[Unit]
Description=Trace System Backend
After=network.target mysqld.service mongod.service redis.service
Wants=redis.service mongod.service

[Service]
Type=simple
User=root
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk
Environment=MONGO_URI=mongodb://127.0.0.1:27017/trace_system
Environment=REDIS_HOST=127.0.0.1
Environment=DB_URL=jdbc:mysql://127.0.0.1:3306/trace_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
Environment=DB_USERNAME=root
Environment=DB_PASSWORD=$DB_PASSWORD
Environment=JWT_SECRET=$JWT_SECRET
WorkingDirectory=/opt/trace-system/trace-backend
ExecStart=/usr/bin/java -jar -Xms512m -Xmx2g target/trace-backend-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
SVC

systemctl daemon-reload
systemctl restart trace-backend
systemctl enable trace-backend
echo "后端服务已启动"

# ---------- 等待后端启动 ----------
echo ""
echo "等待后端启动..."
for i in $(seq 1 30); do
    if curl -s http://127.0.0.1:8080/api/auth/login >/dev/null 2>&1; then
        echo "后端启动成功！"
        break
    fi
    sleep 2
done

echo ""
echo "=============================="
echo "  部署完成！"
echo "  访问地址: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_SERVER_IP')"
echo "  管理端账号: admin / $ADMIN_PASSWORD"
echo "  (首次登录后请在系统内修改密码)"
echo "=============================="
