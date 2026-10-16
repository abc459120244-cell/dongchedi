#!/usr/bin/env bash
# 云服务器（Ubuntu 20.04/22.04）一键部署脚本：安装 Docker → 构建 → 启动 MySQL + 应用 + Nginx
# 用法：sudo bash deploy/deploy-ubuntu.sh
set -euo pipefail

echo "==> 1/5 安装 Docker 与 Docker Compose"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
else
  echo "Docker 已安装，跳过"
fi

echo "==> 2/5 检查项目目录"
if [ ! -f "pom.xml" ]; then
  echo "请在项目根目录执行本脚本（当前缺少 pom.xml）" >&2
  exit 1
fi

echo "==> 3/5 构建镜像"
docker compose -f deploy/docker-compose.yml build

echo "==> 4/5 启动 MySQL / 应用 / Nginx"
docker compose -f deploy/docker-compose.yml up -d

echo "==> 5/5 等待应用就绪"
for i in $(seq 1 30); do
  if curl -fs http://localhost/api/system/health >/dev/null 2>&1; then
    echo "部署成功：http://$(curl -s ifconfig.me 2>/dev/null || echo '服务器IP')/"
    docker compose -f deploy/docker-compose.yml ps
    exit 0
  fi
  sleep 5
done

echo "应用启动超时，请查看日志：docker compose -f deploy/docker-compose.yml logs app" >&2
exit 1
