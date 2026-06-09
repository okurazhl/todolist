package com.smartmemo.user.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 设备响应。
 */
public record DeviceResponse(
        UUID id,
        String deviceType,
        String deviceName,
        String pushToken,
        String pushProvider,
        Instant lastOnlineAt,
        Instant createdAt
) {}
