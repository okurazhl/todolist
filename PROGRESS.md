# 开发进度

> 最后更新：2026-06-06

## MVP Step 1: 基础工程搭建 ✅ 已完成（2026-06-06）

## MVP Step 2: API Gateway + 用户服务 + 鉴权 ✅ 已完成（2026-06-06）

| 子任务 | 状态 | 说明 |
|--------|------|------|
| JDK 21 安装 | ✅ | Eclipse Temurin 21.0.11 |
| refresh_tokens 表 | ✅ | infra/postgres/init.sql |
| 共享类型（Auth） | ✅ | packages/shared-types/src/index.ts |
| user-service 全部代码 | ✅ | 25 个文件，DDD 分层 |
| API Gateway 改造 | ✅ | AuthFilter, RateLimitFilter, 路由 |
| 测试 | ✅ | 4 个测试类，全部通过 |
| 编译验证 | ✅ | user-service + api-gateway 均 BUILD SUCCESSFUL |

### 测试结果

**user-service**: BUILD SUCCESSFUL — 17 tests passed
- JwtTokenProviderTest: 7 passed
- AuthApplicationServiceTest: 5 passed
- AuthControllerTest: 3 passed
- 默认 contextLoads: 1 passed

**api-gateway**: BUILD SUCCESSFUL — 3 tests passed
- AuthFilterTest: 3 passed (public paths, 401 without token, 401 invalid token)

### 文件统计

- 新建 user-service: 35 个文件
- 修改 api-gateway: 4 个文件
- 修改共享类型: 1 个文件
- 修改数据库脚本: 1 个文件
- 新建 PROGRESS.md
