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

-- =============================================
-- MVP Step 3: 备忘录相关表
-- =============================================

-- 分类表
CREATE TABLE IF NOT EXISTS categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    name        VARCHAR(64) NOT NULL,
    color       VARCHAR(7),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    version     INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories (user_id) WHERE deleted_at IS NULL;
CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 标签字典
CREATE TABLE IF NOT EXISTS memo_tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    name        VARCHAR(32) NOT NULL,
    color       VARCHAR(7),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    version     INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_memo_tags_user_id ON memo_tags (user_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_memo_tags_name ON memo_tags (user_id, name) WHERE deleted_at IS NULL;
CREATE TRIGGER update_memo_tags_updated_at
    BEFORE UPDATE ON memo_tags FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 备忘录主表
CREATE TABLE IF NOT EXISTS memos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    title       VARCHAR(256) NOT NULL,
    content     TEXT,
    category_id UUID,
    status      VARCHAR(16) NOT NULL DEFAULT 'active',
    is_pinned   BOOLEAN NOT NULL DEFAULT false,
    remind_at   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    version     INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_memos_user_id ON memos (user_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_memos_status ON memos (user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_memos_category ON memos (user_id, category_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_memos_updated ON memos (user_id, updated_at DESC) WHERE deleted_at IS NULL;
CREATE TRIGGER update_memos_updated_at
    BEFORE UPDATE ON memos FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 备忘录-标签关联
CREATE TABLE IF NOT EXISTS memo_tag_relations (
    memo_id     UUID NOT NULL REFERENCES memos(id),
    tag_id      UUID NOT NULL REFERENCES memo_tags(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (memo_id, tag_id)
);

-- 附件表
CREATE TABLE IF NOT EXISTS memo_attachments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    memo_id      UUID NOT NULL REFERENCES memos(id),
    user_id      UUID NOT NULL REFERENCES users(id),
    file_name    VARCHAR(256) NOT NULL,
    file_size    BIGINT NOT NULL,
    content_type VARCHAR(128),
    object_key   VARCHAR(512) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    version      INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_memo_attachments_memo_id ON memo_attachments (memo_id) WHERE deleted_at IS NULL;

-- =============================================
-- MVP Step 5: ASR 语音转写任务表
-- =============================================
CREATE TABLE IF NOT EXISTS asr_tasks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    file_name        VARCHAR(256) NOT NULL,
    file_size        BIGINT NOT NULL,
    content_type     VARCHAR(128),
    object_key       VARCHAR(512) NOT NULL,
    memo_id          UUID,
    status           VARCHAR(16) NOT NULL DEFAULT 'pending',
    transcribed_text TEXT,
    duration_seconds INTEGER,
    error_message    TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_asr_tasks_user_id ON asr_tasks (user_id);
CREATE INDEX IF NOT EXISTS idx_asr_tasks_status ON asr_tasks (user_id, status);
