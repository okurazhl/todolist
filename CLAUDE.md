# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

智能备忘录系统 — 支持手机、平板、PC、Web、蓝牙硬件和语音输入多端接入。
核心能力：语音识别(ASR)、AI 语义处理、备忘录管理、多端同步、蓝牙设备管理、消息推送、全文搜索。

## 开发铁律

1. **技术选型不可擅自更改** — 所有技术栈已由架构文档锁定，详见 `docs/architecture/智能备忘录系统技术架构文档.md`
2. **必须按 MVP 阶段顺序开发** — 不可跳过 MVP 直接做完整大系统，执行顺序见下方
3. **每个功能必须对应五要素** — 需求、接口、数据表、测试、验收标准，缺一不可
4. **每次只实现一个小闭环** — 完成后运行测试并输出变更说明
5. **开发前必须阅读** `docs/architecture/` 下的技术架构文档和 `docs/prd/` 下的开发规范
6. **完成步骤后必须更新进度** — 每完成一个 MVP 步骤，立即更新下方执行顺序中的进度标记：将已完成步骤标为 `✅ xxx（已完成：日期）`，将 `← 当前步骤` 移到下一步。无需用户提醒。
7. **开始工作前必须先拆解任务** — 用 TodoWrite 将当前步骤拆分为独立子任务，每个子任务有明确的完成标准和测试方法
8. **单个子任务完成后立即测试验收** — 不等到所有代码写完再测。子任务完成 = 代码通过编译/类型检查 + 相关测试通过。测试失败则修好再标记完成
9. **子任务进度必须持久化到文件** — 每完成一个子任务，同步更新 `PROGRESS.md` 中的子任务状态标记（✅/🔄/⬜），确保会话中断后进度不丢失
10. **新增/修改 API 接口或共享类型变量必须同步文档** — 变更后立即更新 `docs/api/接口清单.md`，在对应条目旁注明添加/修改日期。确保接口文档始终与代码一致

## 技术选型（锁定，不可更改）

| 类别 | 选型 |
|------|------|
| 移动端/平板 | Flutter + Dart |
| Web 端 | React + TypeScript + Vite |
| PC 端 | Electron + React + TypeScript |
| 后端主框架 | Java 21 + Spring Boot 4.x |
| API 网关 | Spring Cloud Gateway |
| AI/ASR 服务 | Python 3.11 + FastAPI |
| 主数据库 | PostgreSQL 17（架构文档写18，18未发布前用17） |
| 缓存 | Redis 7+ |
| 搜索引擎 | Elasticsearch / OpenSearch |
| 文件存储 | MinIO（本地）/ OSS / S3（生产） |
| 消息队列 | Kafka |
| ASR 方案 | FunASR 本地 + 云端 ASR 兜底 |
| AI 模型 | DeepSeek API + 模型网关 |

## 系统架构（三层）

```
接入层: Flutter(手机/平板) / React(Web) / Electron(PC) / BLE蓝牙 / 语音
  ↓
服务层: API Gateway → 用户服务 / 备忘录服务 / 同步服务 / ASR服务 / AI服务 / 蓝牙服务 / 推送服务 / 文件服务 / 搜索服务
  ↓
数据层: PostgreSQL / Redis / Elasticsearch / MinIO / Kafka
```

## MVP 执行顺序（严格按此顺序，一次只做一步）

1. ✅ 基础工程搭建（已完成：2026-06-06）
2. ✅ API Gateway + 用户服务 + 鉴权（已完成：2026-06-06）
3. ✅ 备忘录 CRUD + 标签 + 分类 + 附件（已完成：2026-06-06）
4. ✅ Flutter / Web 基础页面（已完成：2026-06-07）
5. ✅ ASR 任务创建 + 录音上传 + 转写回填（已完成：2026-06-07）
6. ✅ AI 语义提炼（已完成：2026-06-08）
7. **基础增量同步 + WebSocket 通知** ← 当前步骤
8. 推送服务基础能力
8. 推送服务基础能力
9. 蓝牙设备绑定 + 数据上报 MVP
10. 测试 + 灰度 + 上线 + 监控

## 核心开发规范

### API 设计
- REST 风格，统一前缀 `/api/v1/`
- 统一响应格式: `{ "code": "OK", "message": "success", "data": {}, "traceId": "uuid" }`
- 分页使用游标: `{ "items": [], "nextCursor": "...", "hasMore": true }`
- 错误码按模块前缀: `AUTH_*`, `USER_*`, `MEMO_*`, `SYNC_*`, `ASR_*`, `AI_*`, `BLE_*`, `PUSH_*`, `FILE_*`, `SYS_*`

### 数据库规范
- 表名: 小写 snake_case 复数 (`memos`, `memo_tags`)
- 主键统一 `id`，外键 `{entity}_id`
- 所有业务表必须含: `id`, `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`, `version`
- 数据库迁移使用 Flyway
- 所有删除默认软删除（`deleted_at`），物理删除需评审

### 后端分层（Spring Boot）
```
api/          → Controller、DTO、入参校验
application/  → 应用服务、事务编排、用例逻辑
domain/       → 实体、值对象、领域规则
infrastructure/ → Repository、外部服务、MQ、缓存
config/       → 配置类、安全、OpenAPI
```
- Controller 不写业务逻辑
- 事务边界在 application 层
- 跨服务调用必须通过 adapter 封装

### 前端规范
- React: TypeScript 严格模式，状态管理用 Zustand/React Query
- API 类型由共享类型包生成，禁止硬编码接口地址
- WebSocket 必须支持重连和心跳

### 消息队列规范
- Topic: `memo.changed`, `sync.changed`, `asr.request`, `ai.request`, `push.request`, `search.index`
- 消息必须含 `eventId`(幂等) 和 `traceId`(链路追踪)
- 消费失败进入重试或死信队列

### 安全要求
- JWT Access Token + Refresh Token
- 全站 HTTPS/WSS
- 文件下载必须校验权限，附件不得公开访问
- 日志不得记录完整 Token、密码、手机号、邮箱、备忘录正文

### 测试最低要求
- 后端核心服务必须有单元测试 + 接口测试
- 同步服务必须覆盖冲突场景
- AI 服务必须覆盖 schema 校验和失败降级

## 本地开发启动

每次新会话或重启后，必须按以下顺序启动 **8 个组件**（基础设施 + 4 后端 + 1 前端）。

### 一键启动

```bash
bash scripts/start-backend.sh   # 启动全部后端（基础设施 + Python + Java + 网关）
bash scripts/stop-backend.sh    # 停止全部后端
```

### 手动启动

#### Phase 1: 基础设施（Docker，5 容器）

```bash
cd infra && docker compose up -d
# 容器: postgres(5432) redis(6379) kafka(9092) elasticsearch(9200) minio(9000)
# 验证: docker ps --format "table {{.Names}}\t{{.Status}}"
```

#### Phase 2: Python 服务（2 个）

```bash
# AI 语义服务 — 端口 8007，依赖 DeepSeek API
cd services/ai-service
python -m venv .venv && source .venv/Scripts/activate && pip install -r requirements.txt -q
uvicorn app.main:app --host 0.0.0.0 --port 8007 &

# ASR 语音转写服务 — 端口 8006，依赖 SiliconFlow API + MinIO
cd services/asr-service
python -m venv .venv && source .venv/Scripts/activate && pip install -r requirements.txt -q
uvicorn app.main:app --host 0.0.0.0 --port 8006 &
# Mock 模式（跳过真实 API 调用）: ASR_ENGINE_MOCK=true uvicorn ...

# 验证: curl http://localhost:8007/actuator/health && curl http://localhost:8006/actuator/health
```

#### Phase 3: Java 服务（3 个）— Spring Boot

> ⚠️ **gradlew 问题**: 项目 `gradlew` 脚本在 Windows 上损坏（内容为 "404: Not Found"），不能使用 `./gradlew`。
> 改用缓存 Gradle 直接运行：
> ```bash
> GRADLE=~/.gradle/wrapper/dists/gradle-8.10-bin/*/gradle-8.10/bin/gradle
> JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
> ```

```bash
# User Service — 端口 8081（先启动，Gateway 和 Memo 依赖它做鉴权）
cd services/user-service && $GRADLE bootRun &

# Memo Service — 端口 8082
cd services/memo-service && $GRADLE bootRun &

# API Gateway — 端口 8080（统一入口，最后启动）
cd services/api-gateway && $GRADLE bootRun &

# 验证: curl http://localhost:8081/actuator/health
#       curl http://localhost:8082/actuator/health
#       curl http://localhost:8080/api/v1/health
```

#### Phase 4: Web 前端

```bash
cd apps/web && npm run dev    # 端口 5173
```

### 服务端口总览

| 服务 | 端口 | 技术 | 依赖 |
|------|------|------|------|
| PostgreSQL | 5432 | Docker | — |
| Redis | 6379 | Docker | — |
| Kafka | 9092 | Docker | Zookeeper |
| Elasticsearch | 9200 | Docker | — |
| MinIO | 9000 | Docker | — |
| **AI Service** | **8007** | Python FastAPI | DeepSeek API |
| **ASR Service** | **8006** | Python FastAPI | SiliconFlow API + MinIO |
| **User Service** | **8081** | Java Spring Boot | PostgreSQL + Redis |
| **Memo Service** | **8082** | Java Spring Boot | PostgreSQL + MinIO |
| **API Gateway** | **8080** | Spring Cloud Gateway | 路由到以上所有服务 |
| Web 前端 | 5173 | Vite + React | API Gateway |

### 关键环境变量

| 变量 | 服务 | 说明 |
|------|------|------|
| `DEEPSEEK_API_KEY` | AI Service | DeepSeek API 密钥 |
| `SILICONFLOW_API_KEY` | ASR Service | 硅基流动 API 密钥 |
| `ASR_ENGINE_MOCK=true` | ASR Service | 使用 Mock 引擎（开发测试用） |
| `JAVA_HOME` | Java 服务 | 指向 JDK 21 安装路径 |

环境变量定义在 `services/.env` 和各服务的 `app/core/config.py` 中。

### 启动验证脚本

```bash
# 逐一检查所有服务健康状态
curl -s http://localhost:8007/actuator/health  # AI Service
curl -s http://localhost:8006/actuator/health  # ASR Service
curl -s http://localhost:8081/actuator/health  # User Service
curl -s http://localhost:8082/actuator/health  # Memo Service
curl -s http://localhost:8080/api/v1/health    # API Gateway
```

### 常见问题

1. **gradlew 404 错误** — 使用缓存 Gradle（路径见上方 Phase 3）
2. **端口被占用** — `netstat -ano | grep ":8006 "` 查 PID，`taskkill //F //PID <PID>` 停止
3. **Python venv 不存在** — 脚本自动创建，或手动执行 Phase 2 的 venv 创建命令
4. **ASR 返回 Mock 结果** — 检查是否设置了 `ASR_ENGINE_MOCK=true`，去掉即可使用真实引擎
5. **AI/ASR 服务 401** — 请求头需要 `X-User-Id`（直接调用）或 `Authorization: Bearer <token>`（经 Gateway）

## 对话语言

使用中文进行所有对话。
