#!/usr/bin/env bash
set -euo pipefail

CONFIG_FILE="src/main/resources/application.yml"
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "配置文件不存在: $CONFIG_FILE"
  exit 1
fi

DB_URL=$(awk '/url: jdbc:mysql:\/\// {print $2}' "$CONFIG_FILE" | head -n1)
DB_USER=$(awk '/username:/ {print $2}' "$CONFIG_FILE" | head -n1)

if [[ -z "${DB_URL:-}" ]]; then
  echo "未在 $CONFIG_FILE 里找到 MySQL JDBC URL"
  exit 1
fi

# 解析 jdbc:mysql://host:port/db?...
URL_BODY=${DB_URL#jdbc:mysql://}
HOST_PORT=${URL_BODY%%/*}
DB_HOST=${HOST_PORT%%:*}
DB_PORT=${HOST_PORT##*:}
if [[ "$DB_HOST" == "$DB_PORT" ]]; then
  DB_PORT=3306
fi

echo "检测配置:"
echo "- JDBC URL: $DB_URL"
echo "- 用户名: ${DB_USER:-<未配置>}"
echo "- 主机: $DB_HOST"
echo "- 端口: $DB_PORT"

echo
echo "开始 TCP 连通性检测..."
if nc -vz "$DB_HOST" "$DB_PORT"; then
  echo "结果: ✅ 数据库端口可达"
else
  echo "结果: ❌ 数据库端口不可达（连接被拒绝或网络不通）"
  exit 2
fi
