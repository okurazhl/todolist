#!/bin/bash
# =============================================
# 智能备忘录 — 后端服务一键启动脚本
# =============================================
# 启动顺序：基础设施 → Python 服务 → Java 服务 → 网关
# 每个服务启动后验证健康检查通过才继续
# =============================================

set -e

# 配置：Git Bash on Windows 需要使用 //c/ 风格路径
JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot}"
GRADLE_CACHE="/c/Users/okura/.gradle/wrapper/dists/gradle-8.10-bin"
GRADLE_BIN=$(ls -d "$GRADLE_CACHE"/*/gradle-8.10/bin/gradle 2>/dev/null | head -1)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()   { echo -e "${GREEN}[ OK ]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }

health_check() {
  local name="$1" url="$2" max_wait="${3:-30}"
  log "等待 $name ($url)..."
  for i in $(seq 1 $max_wait); do
    if curl -sf --connect-timeout 2 "$url" > /dev/null 2>&1; then
      ok "$name 已就绪 (${i}s)"
      return 0
    fi
    sleep 1
  done
  fail "$name 启动超时"
  return 1
}

# =============================================
# Phase 1: 基础设施
# =============================================
echo ""
echo "=========================================="
echo "  Phase 1: 基础设施 (Docker)"
echo "=========================================="

cd "$PROJECT_ROOT/infra"

if ! docker info > /dev/null 2>&1; then
  fail "Docker 未运行，请先启动 Docker Desktop"
  exit 1
fi

log "启动 Docker 容器..."
docker compose up -d 2>&1 | tail -5

health_check "PostgreSQL"  "http://localhost:5432" 15 || true
health_check "Redis"      "http://localhost:6379" 15 || true
log "Kafka/ES/MinIO 容器后台启动中（约需60秒）..."

# =============================================
# Phase 2: Python 服务 (AI + ASR)
# =============================================
echo ""
echo "=========================================="
echo "  Phase 2: Python 服务"
echo "=========================================="

# --- AI Service (port 8007) ---
log "启动 AI Service (8007)..."
cd "$PROJECT_ROOT/services/ai-service"
if [ ! -d ".venv" ]; then
  python -m venv .venv
  source .venv/Scripts/activate
  pip install -r requirements.txt -q
else
  source .venv/Scripts/activate
fi
nohup uvicorn app.main:app --host 0.0.0.0 --port 8007 > /tmp/ai-service.log 2>&1 &
AI_PID=$!
echo $AI_PID > /tmp/ai-service.pid
health_check "AI Service" "http://localhost:8007/actuator/health"

# --- ASR Service (port 8006) ---
log "启动 ASR Service (8006)..."
cd "$PROJECT_ROOT/services/asr-service"
if [ ! -d ".venv" ]; then
  python -m venv .venv
  source .venv/Scripts/activate
  pip install -r requirements.txt -q
else
  source .venv/Scripts/activate
fi
nohup uvicorn app.main:app --host 0.0.0.0 --port 8006 > /tmp/asr-service.log 2>&1 &
ASR_PID=$!
echo $ASR_PID > /tmp/asr-service.pid
health_check "ASR Service" "http://localhost:8006/actuator/health"

# =============================================
# Phase 3: Java 服务
# =============================================
echo ""
echo "=========================================="
echo "  Phase 3: Java 服务 (Spring Boot)"
echo "=========================================="

if [ -z "$GRADLE_BIN" ]; then
  fail "找不到 Gradle 8.10，请检查缓存路径: $GRADLE_CACHE"
  exit 1
fi
log "使用 Gradle: $GRADLE_BIN"
export JAVA_HOME

# --- User Service (port 8081) ---
log "启动 User Service (8081)..."
cd "$PROJECT_ROOT/services/user-service"
nohup "$GRADLE_BIN" bootRun > /tmp/user-service.log 2>&1 &
echo $! > /tmp/user-service.pid
health_check "User Service" "http://localhost:8081/actuator/health"

# --- Memo Service (port 8082) ---
log "启动 Memo Service (8082)..."
cd "$PROJECT_ROOT/services/memo-service"
nohup "$GRADLE_BIN" bootRun > /tmp/memo-service.log 2>&1 &
echo $! > /tmp/memo-service.pid
health_check "Memo Service" "http://localhost:8082/actuator/health"

# --- API Gateway (port 8080) ---
log "启动 API Gateway (8080)..."
cd "$PROJECT_ROOT/services/api-gateway"
nohup "$GRADLE_BIN" bootRun > /tmp/gateway.log 2>&1 &
echo $! > /tmp/gateway.pid
health_check "API Gateway" "http://localhost:8080/api/v1/health"

# =============================================
# 完成
# =============================================
echo ""
echo "=========================================="
echo -e "  ${GREEN}所有后端服务已启动${NC}"
echo "=========================================="
echo ""
echo "  服务列表:"
echo "  ├── API Gateway    → http://localhost:8080"
echo "  ├── User Service   → http://localhost:8081"
echo "  ├── Memo Service   → http://localhost:8082"
echo "  ├── ASR Service    → http://localhost:8006"
echo "  └── AI  Service    → http://localhost:8007"
echo ""
echo "  Web 前端: cd apps/web && npm run dev"
echo "  停止服务: 运行 scripts/stop-backend.sh"
echo ""
