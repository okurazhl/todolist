package com.smartmemo.memo.api.dto;

import java.util.List;

public record MemoListResponse(
        List<MemoResponse> items,
        String nextCursor,
        boolean hasMore
) {}
