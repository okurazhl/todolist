package com.smartmemo.sync.api.dto;

import java.util.List;

public record PullResponse(
        List<MemoSnapshot> items,
        String cursor,
        boolean hasMore
) {}
