# 智能备忘录系统 · Smart Memo

支持手机、平板、PC、Web、蓝牙硬件和语音输入的多端智能备忘录系统。

> **当前阶段**: MVP Step 7 — 基础增量同步 + WebSocket 通知

## 核心能力

| 能力 | 说明 | 状态 |
|------|------|------|
| 📝 备忘录 CRUD | 创建/编辑/删除，支持标签、分类、附件 | ✅ |
| 🎙️ 语音录入 | ASR 语音转写，桌面端绕过 WebView2 麦克风限制 | ✅ |
| 🤖 AI 语义提炼 | 自然语言输入 → 结构化备忘录（DeepSeek） | ✅ |
| 🔐 用户鉴权 | JWT Access Token + Refresh Token | ✅ |
| 🔄 增量同步 | 多端数据同步 + WebSocket 实时通知 | 🚧 |
| 📲 消息推送 | 定时提醒、变更通知 | ⬜ |
| ⌚ 蓝牙设备 | BLE 设备绑定 + 数据上报 | ⬜ |

## 架构总览

```
接入层:  Flutter(手机/平板) / React(Web) / Electron(PC) / BLE蓝牙 / 语音录入
          │
服务层:  API Gateway (8080)
          ├── User Service (8081)      — 用户/鉴权
          ├── Memo Service (8082)      — 备忘录 CRUD
          ├── Sync Service             — 增量同步 + WebSocket
          ├── ASR Service (8006)       — 语音转写 (Python · SiliconFlow)
          ├── AI Service (8007)        — 语义提炼 (Python · DeepSeek)
          ├── Push Service             — 消息推送
          ├── File Service             — 文件管理
          ├── Search Service           — 全文搜索
          └── Bluetooth Service        — 蓝牙设备管理
          │
数据层:  PostgreSQL / Redis / Elasticsearch / MinIO / Kafka
```

## 技术栈

| 类别 | 选型 |
|------|------|
| 移动端/平板 | Flutter + Dart |
| Web 端 | React + TypeScript + Vite |
| PC 端 | Electron + React + TypeScript |
| 桌面端 (轻量) | Python + pywebview (无边框原生窗口) |
| 后端微服务 | Java 21 + Spring Boot 4.x |
| API 网关 | Spring Cloud Gateway |
| AI/ASR 服务 | Python 3.11 + FastAPI |
| 数据库 | PostgreSQL 17 |
| 缓存 | Redis 7 |
| 搜索引擎 | Elasticsearch / OpenSearch |
| 文件存储 | MinIO (本地) / OSS (生产) |
| 消息队列 | Kafka |
| ASR 引擎 | SiliconFlow SenseVoiceSmall (云端) + FunASR (本地兜底) |
| AI 模型 | DeepSeek API |

## 快速开始

### 前置要求

- Docker Desktop（基础设施容器）
- JDK 21（Java 微服务）
- Python 3.11（AI/ASR 服务）
- Node.js 20+（Web 前端）
- Bash shell（Git Bash / WSL）

### 一键启动

```bash
# 启动全部后端（基础设施 + Python + Java + 网关）
bash scripts/start-backend.sh

# 停止全部后端
bash scripts/stop-backend.sh
```

### 手动启动

#### 1. 基础设施

```bash
cd infra && docker compose up -d
# 容器: postgres(5432) redis(6379) kafka(9092) elasticsearch(9200) minio(9000)
```

#### 2. Python 服务

```bash
# AI 语义服务 — 端口 8007（需要 DEEPSEEK_API_KEY）
cd services/ai-service
pip install -r requirements.txt -q
uvicorn app.main:app --host 0.0.0.0 --port 8007 &

# ASR 语音转写 — 端口 8006（需要 SILICONFLOW_API_KEY）
cd services/asr-service
pip install -r requirements.txt -q
uvicorn app.main:app --host 0.0.0.0 --port 8006 &
# Mock 模式: ASR_ENGINE_MOCK=true uvicorn ...
```

#### 3. Java 微服务

```bash
# User Service — 端口 8081（先启动，供 Gateway 和 Memo 做鉴权）
cd services/user-service && ./gradlew bootRun &

# Memo Service — 端口 8082
cd services/memo-service && ./gradlew bootRun &

# API Gateway — 端口 8080（最后启动）
cd services/api-gateway && ./gradlew bootRun &
```

#### 4. Web 前端

```bash
cd apps/web && npm install && npm run dev    # 端口 5173
```

### 桌面端

```bash
cd apps/desktop
start-desktop.bat    # 依赖 Web 前端已启动在 5173 端口
```

### 健康检查

```bash
curl -s http://localhost:8007/actuator/health   # AI Service
curl -s http://localhost:8006/actuator/health   # ASR Service
curl -s http://localhost:8081/actuator/health   # User Service
curl -s http://localhost:8082/actuator/health   # Memo Service
curl -s http://localhost:8080/api/v1/health     # API Gateway
curl -s http://localhost:5173                   # Web 前端
```

## 项目结构

```
todolist/
  apps/                        # 前端应用
    web/                       # React Web 端 (Vite + TypeScript)
    desktop/                   # 桌面端 (Python + pywebview)
    mobile/                    # Flutter 移动端
  services/                    # 后端微服务
    api-gateway/               # Spring Cloud Gateway (8080)
    user-service/              # 用户服务 (8081)
    memo-service/              # 备忘录服务 (8082)
    sync-service/              # 同步服务 + WebSocket
    asr-service/               # ASR 语音转写 (Python · 8006)
    ai-service/                # AI 语义提炼 (Python · 8007)
    push-service/              # 消息推送
    file-service/              # 文件管理
    search-service/            # 全文搜索
    bluetooth-service/         # 蓝牙设备管理
  infra/                       # Docker 编排 + 初始化脚本
  packages/shared-types/       # 共享 TypeScript 类型
  docs/                        # 架构/PRD/API 文档
    architecture/              # 技术架构文档
    prd/                       # 需求文档 / 开发规范
    api/                       # 接口清单
  scripts/                     # 启动/停止/数据库初始化脚本
```

## MVP 进度

| Step | 内容 | 状态 |
|------|------|------|
| 1 | 基础工程搭建 | ✅ 2026-06-06 |
| 2 | API Gateway + 用户服务 + 鉴权 | ✅ 2026-06-06 |
| 3 | 备忘录 CRUD + 标签 + 分类 + 附件 | ✅ 2026-06-06 |
| 4 | Flutter / Web 基础页面 | ✅ 2026-06-07 |
| 5 | ASR 任务创建 + 录音上传 + 转写回填 | ✅ 2026-06-07 |
| 6 | AI 语义提炼 | ✅ 2026-06-08 |
| 7 | 基础增量同步 + WebSocket 通知 | 🚧 进行中 |
| 8 | 推送服务基础能力 | ⬜ |
| 9 | 蓝牙设备绑定 + 数据上报 MVP | ⬜ |
| 10 | 测试 + 灰度 + 上线 + 监控 | ⬜ |

## 环境变量

| 变量 | 服务 | 说明 |
|------|------|------|
| `DEEPSEEK_API_KEY` | AI Service | DeepSeek API 密钥 |
| `SILICONFLOW_API_KEY` | ASR Service | 硅基流动 API 密钥 |
| `ASR_ENGINE_MOCK=true` | ASR Service | 使用 Mock 引擎（跳过 API 调用） |
| `JAVA_HOME` | Java 服务 | JDK 21 安装路径 |

环境变量定义在 `services/.env` 和各服务的 `app/core/config.py` 中。

## 开发指南

详细开发规范参见：
- [`docs/architecture/智能备忘录系统技术架构文档.md`](docs/architecture/智能备忘录系统技术架构文档.md)
- [`docs/prd/智能备忘录系统开发规范.md`](docs/prd/智能备忘录系统开发规范.md)
- [`docs/prd/智能备忘录系统开发需求.md`](docs/prd/智能备忘录系统开发需求.md)
- [`docs/api/接口清单.md`](docs/api/接口清单.md)

### API 规范

- REST 风格，统一前缀 `/api/v1/`
- 统一响应: `{ "code": "OK", "message": "success", "data": {}, "traceId": "uuid" }`
- 分页游标: `{ "items": [], "nextCursor": "...", "hasMore": true }`
- 错误码模块前缀: `AUTH_*`, `USER_*`, `MEMO_*`, `SYNC_*`, `ASR_*`, `AI_*`, `BLE_*`, `PUSH_*`, `FILE_*`, `SYS_*`

## License

MIT
