---
name: start-backend
description: 一键启动智能备忘录所有后端服务（基础设施 + Python + Java + 网关）
metadata:
  type: project
---

# 启动后端服务

启动智能备忘录系统所需的全部后端服务，按依赖顺序依次启动并验证健康状态。

## 服务清单（6个）

| 序号 | 服务 | 端口 | 技术栈 | 健康检查 |
|------|------|------|--------|----------|
| 0 | 基础设施 (Docker) | 5432/6379/9092/9200/9000 | PostgreSQL+Redis+Kafka+ES+MinIO | pg_isready |
| 1 | AI Service | 8007 | Python FastAPI | /actuator/health |
| 2 | ASR Service | 8006 | Python FastAPI | /actuator/health |
| 3 | User Service | 8081 | Java Spring Boot | /actuator/health |
| 4 | Memo Service | 8082 | Java Spring Boot | /actuator/health |
| 5 | API Gateway | 8080 | Java Spring Cloud Gateway | /api/v1/health |

## 启动方式

### 方式 A：一键脚本
```bash
bash scripts/start-backend.sh
```

### 方式 B：手动按顺序启动

**Phase 1 — 基础设施**
```bash
cd infra && docker compose up -d
# 等待 PostgreSQL/Redis/Kafka/ES/MinIO 就绪
```

**Phase 2 — Python 服务**
```bash
# AI 服务 (8007)
cd services/ai-service && source .venv/Scripts/activate && \
  uvicorn app.main:app --host 0.0.0.0 --port 8007 &

# ASR 服务 (8006)
cd services/asr-service && source .venv/Scripts/activate && \
  uvicorn app.main:app --host 0.0.0.0 --port 8006 &
```

**Phase 3 — Java 服务**
```bash
# 使用缓存 Gradle（gradlew 脚本在 Windows 上损坏）
GRADLE=~/.gradle/wrapper/dists/gradle-8.10-bin/*/gradle-8.10/bin/gradle
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"

# User Service (8081)
cd services/user-service && $GRADLE bootRun &

# Memo Service (8082)
cd services/memo-service && $GRADLE bootRun &

# API Gateway (8080)
cd services/api-gateway && $GRADLE bootRun &
```

### 前端（可选）
```bash
cd apps/web && npm run dev    # http://localhost:5173
```

## 停止服务
```bash
bash scripts/stop-backend.sh
```

## 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| Docker Desktop | 最新 | 运行基础设施容器 |
| Java | 21 (Eclipse Adoptium) | Spring Boot 服务 |
| Python | 3.11+ | AI/ASR 服务 |
| Gradle | 8.10 (缓存) | 构建/运行 Java 服务 |
| Node.js | 20 LTS | Web 前端 |

## 依赖关系

```
Docker (PostgreSQL, Redis, Kafka, ES, MinIO)
  ├── AI Service (8007) ──── DeepSeek API
  ├── ASR Service (8006) ─── SiliconFlow API + MinIO
  ├── User Service (8081) ── PostgreSQL + Redis
  ├── Memo Service (8082) ── PostgreSQL + MinIO
  └── API Gateway (8080) ── 路由到以上所有服务
```

## 常见问题

### gradlew 404 错误
项目 gradlew 脚本在 Windows 克隆时损坏（内容为 "404: Not Found"）。
解决方案：直接使用缓存 Gradle `~/.gradle/wrapper/dists/gradle-8.10-bin/.../bin/gradle`

### 端口被占用
```bash
# 查看端口占用
netstat -ano | grep ":8006 "
# 强制终止
taskkill //F //PID <PID>
```

### Python venv 不存在
脚本自动创建。手动创建：
```bash
cd services/ai-service && python -m venv .venv && source .venv/Scripts/activate && pip install -r requirements.txt
cd services/asr-service && python -m venv .venv && source .venv/Scripts/activate && pip install -r requirements.txt
```

### Docker 容器已存在
```bash
cd infra && docker compose up -d  # 增量启动，不会删除数据
```
