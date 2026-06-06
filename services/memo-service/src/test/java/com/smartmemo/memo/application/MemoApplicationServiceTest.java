package com.smartmemo.memo.application;

import com.smartmemo.memo.domain.Memo;
import com.smartmemo.memo.domain.MemoStatus;
import com.smartmemo.memo.infrastructure.persistence.MemoRepository;
import com.smartmemo.memo.infrastructure.persistence.MemoTagRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoApplicationServiceTest {

    @Mock private MemoRepository memoRepository;
    @Mock private MemoTagRelationRepository tagRelationRepository;

    private MemoApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MemoApplicationService(memoRepository, tagRelationRepository);
    }

    @Test
    void shouldCreateMemo() {
        UUID userId = UUID.randomUUID();
        when(memoRepository.save(any(Memo.class))).thenAnswer(inv -> {
            Memo m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        var result = service.create(userId, "测试标题", "测试内容", null, List.of(), false);

        assertNotNull(result);
        assertEquals("测试标题", result.title());
        assertEquals(MemoStatus.active.name(), result.status());
        assertFalse(result.pinned());
    }

    @Test
    void shouldReturnEmptyForNonExistentMemo() {
        UUID userId = UUID.randomUUID();
        UUID memoId = UUID.randomUUID();
        when(memoRepository.findByIdAndUserId(memoId, userId)).thenReturn(Optional.empty());

        var result = service.getById(memoId, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldArchiveAndUnarchive() {
        UUID userId = UUID.randomUUID();
        UUID memoId = UUID.randomUUID();
        Memo memo = createTestMemo(userId);
        memo.setId(memoId);

        when(memoRepository.findByIdAndUserId(memoId, userId)).thenReturn(Optional.of(memo));
        when(memoRepository.save(any())).thenReturn(memo);

        var archived = service.archive(memoId, userId, true);
        assertTrue(archived.isPresent());
        assertEquals(MemoStatus.archived.name(), archived.get().status());

        var unarchived = service.archive(memoId, userId, false);
        assertTrue(unarchived.isPresent());
        assertEquals(MemoStatus.active.name(), unarchived.get().status());
    }

    @Test
    void shouldPinAndUnpin() {
        UUID userId = UUID.randomUUID();
        UUID memoId = UUID.randomUUID();
        Memo memo = createTestMemo(userId);
        memo.setId(memoId);

        when(memoRepository.findByIdAndUserId(memoId, userId)).thenReturn(Optional.of(memo));
        when(memoRepository.save(any())).thenReturn(memo);

        var pinned = service.pin(memoId, userId, true);
        assertTrue(pinned.isPresent());
        assertTrue(pinned.get().pinned());

        var unpinned = service.pin(memoId, userId, false);
        assertTrue(unpinned.isPresent());
        assertFalse(unpinned.get().pinned());
    }

    @Test
    void shouldSoftDelete() {
        UUID userId = UUID.randomUUID();
        UUID memoId = UUID.randomUUID();
        Memo memo = createTestMemo(userId);
        memo.setId(memoId);

        when(memoRepository.findByIdAndUserId(memoId, userId)).thenReturn(Optional.of(memo));
        when(memoRepository.save(any())).thenReturn(memo);

        boolean deleted = service.delete(memoId, userId);
        assertTrue(deleted);
        assertEquals(MemoStatus.deleted, memo.getStatus());
        assertNotNull(memo.getDeletedAt());
    }

    private Memo createTestMemo(UUID userId) {
        Memo memo = new Memo();
        memo.setUserId(userId);
        memo.setTitle("测试");
        memo.setContent("内容");
        memo.setStatus(MemoStatus.active);
        memo.setPinned(false);
        return memo;
    }
}
