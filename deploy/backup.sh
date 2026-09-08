#!/bin/bash
# 溯源系统每日自动备份脚本
# 由 cron 调用：0 2 * * * /opt/trace-system/deploy/backup.sh >> /var/log/trace-backup.log 2>&1
set -euo pipefail

BACKUP_DIR="/data/backups"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

mkdir -p "$BACKUP_DIR"

# 从 systemd 服务文件提取数据库密码
SERVICE_FILE="/etc/systemd/system/trace-backend.service"
if [ ! -f "$SERVICE_FILE" ]; then
    echo "[$(date)] ERROR: Service file not found: $SERVICE_FILE"
    exit 1
fi
DB_PASS=$(grep "^Environment=DB_PASSWORD=" "$SERVICE_FILE" | head -1 | sed 's/Environment=DB_PASSWORD=//')

if [ -z "$DB_PASS" ]; then
    echo "[$(date)] ERROR: Could not extract DB_PASSWORD from service file"
    exit 1
fi

echo "[$(date)] Starting backup..."

# MySQL 全库备份（单事务模式，不锁表）
MYSQL_FILE="$BACKUP_DIR/mysql_$DATE.sql.gz"
mysqldump -uroot -p"$DB_PASS" \
    --single-transaction --routines --triggers --events \
    --databases trace_system \
    | gzip > "$MYSQL_FILE"
echo "[$(date)] MySQL backup: $MYSQL_FILE ($(du -h "$MYSQL_FILE" | cut -f1))"

# MongoDB 备份（码包明细 companion 存储）
MONGO_FILE="$BACKUP_DIR/mongo_$DATE.gz"
mongodump --db trace_system --gzip --archive="$MONGO_FILE"
echo "[$(date)] MongoDB backup: $MONGO_FILE ($(du -h "$MONGO_FILE" | cut -f1))"

# 清理超过保留天数的旧备份
DELETED=$(find "$BACKUP_DIR" -name "*.gz" -mtime +${RETENTION_DAYS} -delete -print | wc -l)
if [ "$DELETED" -gt 0 ]; then
    echo "[$(date)] Cleaned up $DELETED files older than ${RETENTION_DAYS} days"
fi

# 磁盘使用概况
echo "[$(date)] Backup dir usage: $(du -sh "$BACKUP_DIR" | cut -f1)"
echo "[$(date)] Disk free: $(df -h "$BACKUP_DIR" | tail -1 | awk '{print $4}')"
echo "[$(date)] Backup completed successfully."
