package com.smartmemo.sync.application;

import com.smartmemo.sync.api.dto.*;
import com.smartmemo.sync.domain.SyncCursor;
import com.smartmemo.sync.domain.SyncMemo;
import com.smartmemo.sync.infrastructure.persistence.SyncCursorRepository;
import com.smartmemo.sync.infrastructure.persistence.SyncMemoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SyncApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SyncApplicationService.class);
    private static final int PAGE_SIZE = 50;

    private final SyncMemoRepository memoRepository;
    private final SyncCursorRepository cursorRepository;

    public SyncApplicationService(SyncMemoRepository memoRepository, SyncCursorRepository cursorRepository) {
        this.memoRepository = memoRepository;
        this.cursorRepository = cursorRepository;
    }

    /**
     * 增量拉取：返回 cursor 之后的所有变更。
     * cursor 为 null 时返回全部活跃备忘录（首次同步）。
     */
    public PullResponse pull(UUID userId, String cursor) {
        List<SyncMemo> changed;
        if (cursor == null || cursor.isEmpty()) {
            changed = memoRepository.findAllActive(userId, PageRequest.of(0, PAGE_SIZE + 1));
        } else {
            Instant since = Instant.parse(cursor);
            changed = memoRepository.findChangedSince(userId, since, PageRequest.of(0, PAGE_SIZE + 1));
        }

        boolean hasMore = changed.size() > PAGE_SIZE;
        if (hasMore) changed = changed.subList(0, PAGE_SIZE);

        List<MemoSnapshot> items = changed.stream().map(this::toSnapshot).toList();
        String newCursor = items.isEmpty() ? cursor : changed.get(changed.size() - 1).getUpdatedAt().toString();

        // 更新游标
        updateCursor(userId, newCursor != null ? newCursor : Instant.now().toString());

        log.info("Pull: userId={}, cursor={}, items={}, hasMore={}", userId, cursor, items.size(), hasMore);
        return new PullResponse(items, newCursor, hasMore);
    }

    /**
     * 推送本地变更：逐条校验版本号，接受或标记冲突。
     * 简化的冲突策略：版本号不同即冲突，版本号相同则接受。
     */
    @Transactional
    public PushResult push(UUID userId, PushRequest request) {
        List<MemoSnapshot> accepted = new ArrayList<>();
        List<ConflictRecord> conflicts = new ArrayList<>();

        for (MemoSnapshot change : request.changes()) {
            Optional<SyncMemo> serverMemo = memoRepository.findById(change.id());
            if (serverMemo.isEmpty()) {
                // 新备忘录 → 暂不处理（由 memo-service API 负责创建）
                accepted.add(change);
                continue;
            }
            SyncMemo server = serverMemo.get();
            if (Objects.equals(server.getVersion(), change.version())) {
                // 版本匹配 → 接受
                accepted.add(toSnapshot(server));
            } else {
                // 版本冲突
                conflicts.add(new ConflictRecord(change.id(), toSnapshot(server), change));
            }
        }

        log.info("Push: userId={}, accepted={}, conflicts={}", userId, accepted.size(), conflicts.size());
        return new PushResult(accepted, conflicts);
    }

    /**
     * 更新用户同步游标。
     */
    public void updateCursor(UUID userId, String cursor) {
        SyncCursor sc = cursorRepository.findByUserId(userId)
                .orElseGet(() -> new SyncCursor(userId, cursor));
        sc.setCursor(cursor);
        sc.setUpdatedAt(Instant.now());
        cursorRepository.save(sc);
    }

    private MemoSnapshot toSnapshot(SyncMemo m) {
        return new MemoSnapshot(
                m.getId(), m.getTitle(), m.getContent(), m.getCategoryId(),
                m.getStatus(), m.isPinned(), m.getRemindAt(),
                m.getCreatedAt(), m.getUpdatedAt(), m.getVersion()
        );
    }
}
