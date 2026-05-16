#!/bin/sh
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "当前目录不是 Git 仓库：$ROOT_DIR" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "未找到 docker 命令，请先安装 Docker。" >&2
  exit 1
fi

echo "==> 更新代码"
git pull

echo "==> 关闭 Docker Compose"
docker compose down

echo "==> 构建并启动 Docker Compose"
docker compose up -d --build

echo "==> 当前服务状态"
docker compose ps
