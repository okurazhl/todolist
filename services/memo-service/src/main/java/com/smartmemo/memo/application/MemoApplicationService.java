package com.smartmemo.memo.application;

import com.smartmemo.memo.domain.Memo;
import com.smartmemo.memo.domain.MemoStatus;
import com.smartmemo.memo.infrastructure.persistence.MemoRepository;
import com.smartmemo.memo.infrastructure.persistence.MemoTagRelationRepository;
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

    public MemoApplicationService(MemoRepository memoRepository, MemoTagRelationRepository tagRelationRepository) {
        this.memoRepository = memoRepository;
        this.tagRelationRepository = tagRelationRepository;
    }

    @Transactional
    public MemoResult create(UUID userId, String title, String content, UUID categoryId, List<UUID> tagIds, boolean pinned) {
        Memo memo = new Memo();
        memo.setUserId(userId);
        memo.setTitle(title);
        memo.setContent(content);
        memo.setCategoryId(categoryId);
        memo.setPinned(pinned);
        memo.setStatus(MemoStatus.active);

        Memo saved = memoRepository.save(memo);
        if (tagIds != null && !tagIds.isEmpty()) {
            tagRelationRepository.replaceRelations(saved.getId(), tagIds);
        }
        log.info("Memo created: id={}, userId={}", saved.getId(), userId);
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
        return Optional.of(MemoResult.from(memo, currentTagIds));
    }

    @Transactional
    public boolean delete(UUID memoId, UUID userId) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return false;
        memo.setStatus(MemoStatus.deleted);
        memo.setDeletedAt(Instant.now());
        memoRepository.save(memo);
        return true;
    }

    public Optional<MemoResult> getById(UUID memoId, UUID userId) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId).orElse(null);
        if (memo == null) return Optional.empty();
        List<UUID> tagIds = tagRelationRepository.findTagIdsByMemoId(memoId);
        return Optional.of(MemoResult.from(memo, tagIds));
    }

    public ListResult list(UUID userId, String status, UUID categoryId, UUID tagId, String cursor, int limit) {
        List<Memo> memos;
        if (tagId != null) {
            memos = memoRepository.findByUserIdAndTag(userId, tagId, PageRequest.of(0, limit + 1));
        } else if (categoryId != null) {
            memos = memoRepository.findByUserIdAndCategory(userId, categoryId, PageRequest.of(0, limit + 1));
        } else if (status != null) {
            MemoStatus ms = MemoStatus.valueOf(status);
            memos = memoRepository.findByUserIdAndStatus(userId, ms, PageRequest.of(0, limit + 1));
        } else {
            memos = memoRepository.findByUserIdCursor(userId, cursor, PageRequest.of(0, limit + 1));
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
                              Instant createdAt, Instant updatedAt) {
        public static MemoResult from(Memo m, List<UUID> tagIds) {
            return new MemoResult(m.getId(), m.getUserId(), m.getTitle(), m.getContent(),
                    m.getCategoryId(), m.getStatus().name(), m.isPinned(), tagIds,
                    m.getCreatedAt(), m.getUpdatedAt());
        }
    }

    public record ListResult(List<MemoResult> items, String nextCursor, boolean hasMore) {}
}
