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
2. **API Gateway + 用户服务 + 鉴权** ← 当前步骤
3. 备忘录 CRUD + 标签 + 分类 + 附件
4. Flutter / Web 基础页面
5. ASR 任务创建 + 录音上传 + 转写回填
6. AI 摘要 + 标签推荐 + 待办提取
7. 基础增量同步 + WebSocket 通知
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

### 接口设计原则（降低耦合）
- **DTO 与 Entity 分离** — Controller 只使用专用 DTO，禁止直接返回/接收 Entity
- **分层隔离** — Controller → Service 接口（非具体类），Service → Repository 接口
- **Request/Response 独立** — 每个接口使用专属的 Request 和 Response DTO，不共用
- **字段最小化** — DTO 只暴露接口需要的字段，不泄露底层表结构
- **跨服务通过 adapter** — 调用外部服务必须封装 adapter，不直接在业务代码中发起 HTTP 调用

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

## 对话语言

使用中文进行所有对话。
