# Git Hook 自动部署配置指南 - trace-system

## 📋 前置准备

### 1. 获取服务器登录信息

**SSH 密码：**
- 登录阿里云控制台 → ECS → 实例列表
- 找到 IP 为 `8.152.162.118` 的实例
- 点击"更多" → "密码/密钥对" → "重置实例密码"
- 设置新密码并记录

**SSH Key（可选）：**
- 如果你希望使用 SSH Key 登录（更安全）
- 本地公钥位置：`~/.ssh/id_rsa.pub`
- 将公钥内容添加到服务器的 `~/.ssh/authorized_keys`

---

## 🚀 部署步骤

### 步骤 1：上传配置脚本到服务器

```bash
# 在本地执行（Mac/Linux）
scp /Users/ron/Qoder/trace-system/deploy/setup-git-hook.sh root@8.152.162.118:/tmp/

# 如果提示输入密码，输入你刚重置的服务器密码
```

### 步骤 2：SSH 登录服务器并执行配置脚本

```bash
# SSH 登录
ssh root@8.152.162.118

# 执行配置脚本
chmod +x /tmp/setup-git-hook.sh
/tmp/setup-git-hook.sh

# 等待脚本执行完成，看到 "✅ Git Hook 配置完成！" 表示成功
```

### 步骤 3：在本地添加远程仓库

```bash
# 进入 trace-system 目录
cd /Users/ron/Qoder/trace-system

# 添加 deploy 远程（用于推送到服务器）
git remote add deploy root@8.152.162.118:/opt/git-repos/trace-system.git

# 验证远程仓库
git remote -v
# 应该看到：
# origin  git@github.com:ron91236/huace-traceability-system.git (fetch)
# origin  git@github.com:ron91236/huace-traceability-system.git (push)
# gitee   git@gitee.com:ron7555/huace-traceability-system.git (fetch)
# gitee   git@gitee.com:ron7555/huace-traceability-system.git (push)
# deploy  root@8.152.162.118:/opt/git-repos/trace-system.git (fetch)
# deploy  root@8.152.162.118:/opt/git-repos/trace-system.git (push)
```

### 步骤 4：测试自动部署

```bash
# 修改一些代码（比如修改 README 或某个文件）
echo "# Test Auto Deploy" >> README.md

# 提交并推送
git add README.md
git commit -m "test: 测试自动部署"

# 推送到服务器触发自动部署
git push deploy master

# 观察输出，应该看到部署过程的日志
```

---

## ✅ 验证部署

推送完成后，访问以下地址验证：

1. **前端页面：** http://8.152.162.118
2. **后端 API：** http://8.152.162.118/api/auth/login
3. **查看部署日志：** 
   ```bash
   ssh root@8.152.162.118
   tail -f /opt/git-repos/trace-system.git/logs/post-receive.log
   ```

---

## 🔧 故障排查

### 问题 1：SSH 连接失败

**原因：** 密码错误或 SSH Key 未配置

**解决：**
```bash
# 检查是否能 ping 通
ping 8.152.162.118

# 尝试 SSH 连接（加 -v 查看详细日志）
ssh -v root@8.152.162.118
```

### 问题 2：推送时提示权限不足

**原因：** bare 仓库或部署目录权限不正确

**解决：**
```bash
ssh root@8.152.162.118
chown -R root:root /opt/git-repos/trace-system.git
chown -R root:root /opt/trace-system
```

### 问题 3：部署后服务未启动

**原因：** 构建失败或服务启动异常

**解决：**
```bash
ssh root@8.152.162.118

# 查看后端日志
journalctl -u trace-backend -n 100 --no-pager

# 查看 Nginx 日志
tail -f /var/log/nginx/error.log

# 手动重启服务
systemctl restart trace-backend
systemctl restart nginx
```

---

##  日常使用流程

以后每次修改代码后：

```bash
# 1. 修改代码
# ... 编辑文件 ...

# 2. 提交到本地
git add .
git commit -m "feat: 你的改动描述"

# 3. 推送到 GitHub/Gitee（备份）
git push origin master
git push gitee master

# 4. 推送到服务器（触发自动部署）
git push deploy master

# 等待 2-5 分钟，部署完成后即可访问
```

---

## ️ 注意事项

1. **首次部署时间较长**：需要安装依赖、构建前后端，可能需要 5-10 分钟
2. **后续部署较快**：只更新变更部分，通常 2-5 分钟
3. **确保服务器资源充足**：至少 2GB 内存用于 Maven 构建
4. **监控磁盘空间**：定期清理 `/opt/trace-system/trace-backend/target` 下的旧 jar 包

---

## 🎯 下一步优化（可选）

1. **添加部署通知**：在 post-receive hook 中添加钉钉/企业微信通知
2. **回滚机制**：保留最近 3 个版本的备份，支持快速回滚
3. **健康检查**：部署后自动运行测试用例
4. **多环境部署**：区分 dev/test/prod 环境

如有问题，随时告诉我！
