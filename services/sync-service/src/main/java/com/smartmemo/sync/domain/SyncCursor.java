package com.smartmemo.sync.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 用户同步游标。
 * 每个用户一个 cursor，记录最后同步的时间点。
 */
@Entity
@Table(name = "sync_cursors")
public class SyncCursor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String cursor;  // ISO-8601 timestamp string

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SyncCursor() {}

    public SyncCursor(UUID userId, String cursor) {
        this.userId = userId;
        this.cursor = cursor;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
