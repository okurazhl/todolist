package com.smartmemo.sync.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MemoSnapshot(
        UUID id,
        String title,
        String content,
        UUID categoryId,
        String status,
        boolean pinned,
        Instant remindAt,
        Instant createdAt,
        Instant updatedAt,
        Integer version
) {}
