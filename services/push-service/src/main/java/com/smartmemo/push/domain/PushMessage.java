package com.smartmemo.push.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_messages")
public class PushMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "memo_id")
    private UUID memoId;

    @Column(nullable = false, length = 32)
    private String type;  // reminder, sync_changed, asr_done, ai_done, system

    @Column(nullable = false, length = 256)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PushMessage() {}

    public PushMessage(UUID userId, UUID memoId, String type, String title, String body) {
        this.userId = userId;
        this.memoId = memoId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getMemoId() { return memoId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
}
