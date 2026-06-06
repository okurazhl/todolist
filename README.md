# 智能备忘录系统 (Smart Memo)

支持手机、平板、PC、Web、蓝牙硬件和语音输入的多端智能备忘录系统。

## 技术栈

| 层级 | 技术 |
|------|------|
| 移动端/平板 | Flutter + Dart |
| Web 端 | React + TypeScript + Vite |
| PC 端 | Electron + React + TypeScript |
| 后端服务 | Java 21 + Spring Boot 4.x |
| API 网关 | Spring Cloud Gateway |
| AI/ASR | Python 3.11 + FastAPI |
| 数据库 | PostgreSQL 17 |
| 缓存 | Redis 7 |
| 搜索 | Elasticsearch |
| 文件存储 | MinIO |
| 消息队列 | Kafka |

## 快速开始

### 1. 启动基础设施

```bash
cd infra
docker compose up -d
```

### 2. 启动 API Gateway

```bash
cd services/api-gateway
./gradlew bootRun
```

### 3. 启动 Web 端

```bash
cd apps/web
npm install
npm run dev
```

### 4. 验证

访问 http://localhost:5173 查看健康检查页面。

## 项目结构

```
smart-memo/
  apps/           # 前端应用
    mobile/       # Flutter 移动端
    web/          # React Web 端
    desktop/      # Electron PC 端
  services/       # 后端微服务
    api-gateway/  # API 网关
    user-service/ # 用户服务
    memo-service/ # 备忘录服务
    ...
  packages/       # 共享包
    shared-types/ # 共享 TypeScript 类型
  infra/          # 基础设施配置
    docker-compose.yml
    postgres/
    ...
  docs/           # 文档
    architecture/
    api/
    prd/
  scripts/        # 脚本
    init-db/
```

## 开发阶段

当前处于 MVP 第一阶段：基础工程搭建。
