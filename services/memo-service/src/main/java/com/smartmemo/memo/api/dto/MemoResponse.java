package com.smartmemo.memo.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoResponse(
        UUID id,
        String title,
        String content,
        UUID categoryId,
        String status,
        boolean isPinned,
        List<UUID> tagIds,
        Instant remindAt,
        Instant createdAt,
        Instant updatedAt
) {}
