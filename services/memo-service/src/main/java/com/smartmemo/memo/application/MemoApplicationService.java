package com.smartmemo.memo.application;

import com.smartmemo.memo.domain.Memo;
import com.smartmemo.memo.domain.MemoStatus;
import com.smartmemo.memo.infrastructure.persistence.MemoRepository;
import com.smartmemo.memo.infrastructure.persistence.MemoTagRelationRepository;
import com.smartmemo.memo.infrastructure.sync.SyncNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MemoApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MemoApplicationService.class);

    private final MemoRepository memoRepository;
    private final MemoTagRelationRepository tagRelationRepository;
    private final SyncNotifier syncNotifier;

    public MemoApplicationService(MemoRepository memoRepository, MemoTagRelationRepository tagRelationRepository,
                                   SyncNotifier syncNotifier) {
        this.memoRepository = memoRepository;
        this.tagRelationRepository = tagRelationRepository;
        this.syncNotifier = syncNotifier;
    }

    @Transactional
    public MemoResult create(UUID userId, String title, String content, UUID categoryId, List<UUID> tagIds, boolean pinned, Instant remindAt) {
        Memo memo = new Memo();
        memo.setUserId(userId);
        memo.setTitle(title);
        memo.setContent(content);
        memo.setCategoryId(categoryId);
        memo.setPinned(pinned);
        memo.setRemindAt(remindAt);
        memo.setStatus(MemoStatus.active);

        Memo saved = memoRepository.save(memo);
        if (tagIds != null && !tagIds.isEmpty()) {
            tagRelationRepository.replaceRelations(saved.getId(), tagIds);
        }
        log.info("Memo created: id={}, userId={}", saved.getId(), userId);
        syncNotifier.notifyChanged(userId, saved.getId(), "memo_created");
        return MemoResult.from(saved, tagIds);
    }

    @Transactional
    public Optional<MemoResult> update(UUID memoId, UUID userId, String title, String content, UUID categoryId, List<UUID> tagIds, Boolean pinned) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return Optional.empty();

        if (title != null) memo.setTitle(title);
        if (content != null) memo.setContent(content);
        if (categoryId != null) memo.setCategoryId(categoryId);
        if (pinned != null) memo.setPinned(pinned);

        memoRepository.save(memo);

        if (tagIds != null) {
            tagRelationRepository.replaceRelations(memoId, tagIds);
        }
        List<UUID> currentTagIds = tagRelationRepository.findTagIdsByMemoId(memoId);
        syncNotifier.notifyChanged(userId, memoId, "memo_updated");
        return Optional.of(MemoResult.from(memo, currentTagIds));
    }

    @Transactional
    public boolean delete(UUID memoId, UUID userId) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return false;
        memo.setStatus(MemoStatus.deleted);
        memo.setDeletedAt(Instant.now());
        memoRepository.save(memo);
        syncNotifier.notifyChanged(userId, memoId, "memo_deleted");
        return true;
    }

    public Optional<MemoResult> getById(UUID memoId, UUID userId) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return Optional.empty();
        List<UUID> tagIds = tagRelationRepository.findTagIdsByMemoId(memoId);
        return Optional.of(MemoResult.from(memo, tagIds));
    }

    @Transactional
    public Optional<MemoResult> pin(UUID memoId, UUID userId, boolean pinned) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return Optional.empty();
        memo.setPinned(pinned);
        memoRepository.save(memo);
        List<UUID> tagIds = tagRelationRepository.findTagIdsByMemoId(memoId);
        return Optional.of(MemoResult.from(memo, tagIds));
    }

    @Transactional
    public Optional<MemoResult> archive(UUID memoId, UUID userId, boolean archive) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return Optional.empty();
        memo.setStatus(archive ? MemoStatus.archived : MemoStatus.active);
        memoRepository.save(memo);
        List<UUID> tagIds = tagRelationRepository.findTagIdsByMemoId(memoId);
        return Optional.of(MemoResult.from(memo, tagIds));
    }

    public record MemoResult(UUID id, UUID userId, String title, String content, UUID categoryId,
                              String status, boolean pinned, List<UUID> tagIds,
                              Instant remindAt, Instant createdAt, Instant updatedAt) {
        public static MemoResult from(Memo m, List<UUID> tagIds) {
            return new MemoResult(m.getId(), m.getUserId(), m.getTitle(), m.getContent(),
                    m.getCategoryId(), m.getStatus().name(), m.isPinned(), tagIds,
                    m.getRemindAt(), m.getCreatedAt(), m.getUpdatedAt());
        }
    }

    @Transactional
    public Optional<MemoResult> complete(UUID memoId, UUID userId, boolean completed) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return Optional.empty();
        memo.setStatus(completed ? MemoStatus.completed : MemoStatus.active);
        memoRepository.save(memo);
        syncNotifier.notifyChanged(userId, memoId, completed ? "memo_completed" : "memo_reactivated");
        List<UUID> tagIds = tagRelationRepository.findTagIdsByMemoId(memoId);
        return Optional.of(MemoResult.from(memo, tagIds));
    }

    public long countReminders(UUID userId) {
        return memoRepository.countUpcomingReminders(userId);
    }

    public ListResult list(UUID userId, String status, UUID categoryId, UUID tagId,
                            String cursor, String remindBefore, int limit) {
        List<Memo> memos;
        if (tagId != null) {
            memos = memoRepository.findByUserIdAndTag(userId, tagId, PageRequest.of(0, limit + 1));
        } else if (categoryId != null) {
            memos = memoRepository.findByUserIdAndCategory(userId, categoryId, PageRequest.of(0, limit + 1));
        } else if (remindBefore != null) {
            Instant before = Instant.parse(remindBefore);
            memos = memoRepository.findByUserIdAndRemindBefore(userId, before, PageRequest.of(0, limit + 1));
        } else if (status != null) {
            MemoStatus ms = MemoStatus.valueOf(status);
            memos = memoRepository.findByUserIdAndStatus(userId, ms, PageRequest.of(0, limit + 1));
        } else if (cursor != null && !cursor.isEmpty()) {
            memos = memoRepository.findByUserIdAndCursor(userId, Instant.parse(cursor), PageRequest.of(0, limit + 1));
        } else {
            memos = memoRepository.findByUserId(userId, PageRequest.of(0, limit + 1));
        }

        boolean hasMore = memos.size() > limit;
        if (hasMore) memos = memos.subList(0, limit);

        List<MemoResult> items = memos.stream()
                .map(m -> MemoResult.from(m, tagRelationRepository.findTagIdsByMemoId(m.getId())))
                .toList();

        String nextCursor = items.isEmpty() ? null
                : items.get(items.size() - 1).updatedAt().toString();

        return new ListResult(items, nextCursor, hasMore);
    }

    public record ListResult(List<MemoResult> items, String nextCursor, boolean hasMore) {}
}
