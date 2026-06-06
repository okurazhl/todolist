package com.smartmemo.memo.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID memoId,
        String fileName,
        long fileSize,
        String contentType,
        Instant createdAt
) {}
