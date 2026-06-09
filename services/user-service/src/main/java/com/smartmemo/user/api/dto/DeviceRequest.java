package com.smartmemo.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 设备绑定请求。
 */
public record DeviceRequest(
        @NotBlank String deviceType,
        @Size(max = 128) String deviceName,
        String pushToken,
        String pushProvider
) {}
