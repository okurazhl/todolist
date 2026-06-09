package com.smartmemo.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求。
 */
public record UpdateUserRequest(
        @Email String email,
        @Size(max = 20) String phone
) {}
