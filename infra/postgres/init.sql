-- =============================================
-- 智能备忘录系统 — 数据库初始化脚本
-- =============================================

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 设置时区
SET timezone = 'Asia/Shanghai';

-- =============================================
-- 用户表（为 MVP Step 2 做准备）
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(64)  NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(20),
    password_hash   VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'active',  -- active, disabled, deleted
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER      NOT NULL DEFAULT 1
);

-- 唯一约束（排除软删除记录）
CREATE UNIQUE INDEX idx_users_username ON users (username) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_users_email    ON users (email)    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_users_phone    ON users (phone)    WHERE deleted_at IS NULL;

-- =============================================
-- 用户设备表
-- =============================================
CREATE TABLE IF NOT EXISTS user_devices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id),
    device_type     VARCHAR(32)  NOT NULL,  -- ios, android, web, desktop
    device_name     VARCHAR(128),
    push_token      TEXT,
    push_provider   VARCHAR(32),            -- apns, fcm, huawei, xiaomi
    last_online_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    version         INTEGER      NOT NULL DEFAULT 1
);

CREATE INDEX idx_user_devices_user_id ON user_devices (user_id) WHERE deleted_at IS NULL;

-- =============================================
-- 自动更新 updated_at 触发器函数
-- =============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- =============================================
-- Refresh Token 表
-- 存储 Refresh Token 的 SHA-256 哈希，支持轮换和撤销
-- =============================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    token_hash    VARCHAR(255) NOT NULL,
    device_id     UUID,
    expires_at    TIMESTAMPTZ NOT NULL,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- =============================================
-- 自动更新 updated_at 触发器函数
-- =============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为已有表创建触发器
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_devices_updated_at
    BEFORE UPDATE ON user_devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
