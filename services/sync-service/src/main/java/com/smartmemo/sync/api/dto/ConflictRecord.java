package com.smartmemo.sync.api.dto;

import java.util.UUID;

public record ConflictRecord(
        UUID memoId,
        MemoSnapshot serverVersion,
        MemoSnapshot clientVersion
) {}
