#!/bin/bash
# =============================================
# 智能备忘录 — 停止所有后端服务
# =============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "停止后端服务..."

# 按端口杀进程（Windows 兼容）
for port in 8080 8081 8082 8006 8007; do
  PID=$(netstat -ano 2>/dev/null | grep ":$port " | grep LISTENING | awk '{print $NF}' | head -1)
  if [ -n "$PID" ] && [ "$PID" != "0" ]; then
    taskkill //F //PID "$PID" 2>/dev/null && echo -e "${GREEN}[STOPPED]${NC} port $port (PID $PID)" || echo -e "${RED}[FAIL]${NC} port $port"
  else
    echo -e "${GREEN}[ FREE ]${NC} port $port"
  fi
done

# 清理 PID 文件
rm -f /tmp/ai-service.pid /tmp/asr-service.pid /tmp/user-service.pid /tmp/memo-service.pid /tmp/gateway.pid

echo "完成。Docker 容器保持运行（如需停止: cd infra && docker compose down）"
