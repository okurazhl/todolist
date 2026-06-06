package com.smartmemo.user.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户信息响应。
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        String phone,
        String status,
        Instant createdAt
) {}
