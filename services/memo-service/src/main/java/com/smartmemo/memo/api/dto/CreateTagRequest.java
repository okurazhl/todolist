package com.smartmemo.memo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank @Size(max = 32) String name,
        @Size(max = 7) String color
) {}
