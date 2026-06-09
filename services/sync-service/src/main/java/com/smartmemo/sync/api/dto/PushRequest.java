package com.smartmemo.sync.api.dto;

import java.util.List;

public record PushRequest(
        List<MemoSnapshot> changes,
        String baseCursor
) {}
