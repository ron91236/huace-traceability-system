# Trace-System 完整部署指南

## 📋 当前状态

✅ **前端已可用**：http://8.152.162.118  
❌ **后端服务未启动**：需要 Java 17 编译和运行

---

## 🔧 解决方案：本地构建 + 上传部署

### 步骤 1：安装 Java 17（本地 Mac）

```bash
# 方法 A：使用 Homebrew（推荐）
brew install --cask temurin17

# 方法 B：手动下载（如果 brew 有问题）
cd /tmp
curl -L "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_x64_mac_hotspot_17.0.12_7.tar.gz" -o openjdk17.tar.gz
tar xzf openjdk17.tar.gz
sudo mv jdk-17.0.12+7 /Library/Java/JavaVirtualMachines/temurin-17.jdk
rm openjdk17.tar.gz
```

验证安装：
```bash
/usr/libexec/java_home -V  # 应该看到 Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
java -version  # 应该显示 openjdk version "17.x.x"
```

### 步骤 2：用 Java 17 构建后端

```bash
cd /Users/ron/Qoder/trace-system/trace-backend

# 设置 Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 构建 jar 包
mvn clean package -DskipTests

# 检查是否生成成功
ls -lh target/trace-backend-1.0.0.jar
```

### 步骤 3：上传 jar 包到服务器

```bash
# 上传 jar 包
scp target/trace-backend-1.0.0.jar root@8.152.162.118:/opt/trace-system/trace-backend/target/

# SSH 登录服务器
ssh root@8.152.162.118

# 重启后端服务
systemctl restart trace-backend

# 等待 10 秒
sleep 10

# 检查服务状态
systemctl status trace-backend

# 测试 API
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 退出
exit
```

### 步骤 4：访问系统

- **前端页面**：http://8.152.162.118
- **默认账号**：admin / admin123

---

## 🚀 自动化部署脚本（可选）

我已经创建了自动化脚本，你可以在安装好 Java 17 后直接运行：

```bash
cd /Users/ron/Qoder/trace-system

# 确保使用 Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 运行自动部署脚本
./deploy/auto-deploy-full.exp
```

---

## 🔍 故障排查

### 问题 1：Java 版本不对

**症状**：`mvn clean package` 报错 `无效的标记: --release`

**解决**：
```bash
# 检查当前 Java 版本
java -version

# 切换到 Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
java -version  # 确认是 17

# 重新构建
mvn clean package -DskipTests
```

### 问题 2：后端服务启动失败

**症状**：`systemctl status trace-backend` 显示 failed

**解决**：
```bash
ssh root@8.152.162.118

# 查看错误日志
journalctl -u trace-backend -n 50 --no-pager

# 检查 jar 包是否存在
ls -lh /opt/trace-system/trace-backend/target/trace-backend-1.0.0.jar

# 如果不存在，重新上传
exit
scp target/trace-backend-1.0.0.jar root@8.152.162.118:/opt/trace-system/trace-backend/target/
ssh root@8.152.162.118
systemctl restart trace-backend
```

### 问题 3：数据库连接失败

**症状**：API 返回 500 错误

**解决**：
```bash
ssh root@8.152.162.118

# 检查 MySQL 状态
systemctl status mysqld

# 检查数据库是否存在（密码以 systemd 服务中的 DB_PASSWORD 为准，不在此处明文记录）
mysql -u root -p -e "SHOW DATABASES;"

# 如果数据库不存在，重新初始化
mysql -u root -p < /opt/trace-system/sql/V1__init_schema.sql
```

---

##  日常开发流程

以后每次修改代码后：

```bash
# 1. 提交代码到 Git
git add .
git commit -m "你的改动描述"

# 2. 推送到 GitHub/Gitee（备份）
git push origin master
git push gitee master

# 3. 本地构建（需要 Java 17）
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd trace-backend
mvn clean package -DskipTests

# 4. 上传到服务器
scp target/trace-backend-1.0.0.jar root@8.152.162.118:/opt/trace-system/trace-backend/target/

# 5. SSH 登录并重启服务
ssh root@8.152.162.118
systemctl restart trace-backend
exit

# 6. 等待 10 秒后访问系统
```

---

## ️ Git Hook 自动部署说明

之前配置的 Git Hook 自动部署因为 Java 版本问题暂时无法使用。等你在服务器上配置好 Java 17 后，可以重新启用：

```bash
# 在服务器上执行
ssh root@8.152.162.118

# 设置 JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
source ~/.bashrc

# 重新配置 Git Hook
/tmp/setup-git-hook.sh

exit
```

之后就可以通过 `git push deploy master` 自动部署了。

---

## 📞 需要帮助？

如果遇到问题，随时告诉我！我会帮你解决。
