package com.smartmemo.memo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateMemoRequest(
        @NotBlank @Size(max = 256) String title,
        @Size(max = 50000) String content,
        UUID categoryId,
        List<UUID> tagIds,
        boolean isPinned,
        Instant remindAt
) {}
