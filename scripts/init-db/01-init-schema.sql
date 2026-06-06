-- =============================================
-- 智能备忘录系统 — 初始 Schema
-- 用于 Flyway 迁移脚本的 SQL 模板
-- =============================================

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

SET timezone = 'Asia/Shanghai';

-- 创建 schema
CREATE SCHEMA IF NOT EXISTS smartmemo;
