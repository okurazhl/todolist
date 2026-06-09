package com.smartmemo.sync.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 备忘录同步视图（只读）。
 * 映射 memos 表，用于增量拉取变更。
 */
@Entity
@Table(name = "memos")
public class SyncMemo {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "category_id")
    private UUID categoryId;

    private String status;

    @Column(name = "is_pinned")
    private boolean pinned;

    @Column(name = "remind_at")
    private Instant remindAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Integer version;

    // getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public UUID getCategoryId() { return categoryId; }
    public String getStatus() { return status; }
    public boolean isPinned() { return pinned; }
    public Instant getRemindAt() { return remindAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Integer getVersion() { return version; }
}
