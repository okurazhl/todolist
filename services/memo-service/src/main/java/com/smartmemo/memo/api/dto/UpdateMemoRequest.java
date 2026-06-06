package com.smartmemo.memo.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateMemoRequest(
        @Size(max = 256) String title,
        @Size(max = 50000) String content,
        UUID categoryId,
        List<UUID> tagIds,
        Boolean isPinned
) {}
