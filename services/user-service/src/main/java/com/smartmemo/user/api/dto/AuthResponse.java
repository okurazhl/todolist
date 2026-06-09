package com.smartmemo.user.api.dto;

/**
 * 认证响应（登录/刷新后返回）。
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
