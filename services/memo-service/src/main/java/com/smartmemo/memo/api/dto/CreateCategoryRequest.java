package com.smartmemo.memo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 7) String color,
        int sortOrder
) {}
