package com.smartmemo.sync.api.dto;

import java.util.List;

public record PushResult(
        List<MemoSnapshot> accepted,
        List<ConflictRecord> conflicts
) {}
