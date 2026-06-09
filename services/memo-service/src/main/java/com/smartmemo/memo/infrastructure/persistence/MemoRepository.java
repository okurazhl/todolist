package com.smartmemo.memo.infrastructure.persistence;

import com.smartmemo.memo.domain.Memo;
import com.smartmemo.memo.domain.MemoStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoRepository extends JpaRepository<Memo, UUID> {

    @Query("SELECT m FROM Memo m WHERE m.userId = :userId AND m.deletedAt IS NULL AND m.status = :status ORDER BY m.pinned DESC, m.updatedAt DESC")
    List<Memo> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") MemoStatus status, Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.userId = :userId AND m.deletedAt IS NULL AND m.categoryId = :categoryId ORDER BY m.pinned DESC, m.updatedAt DESC")
    List<Memo> findByUserIdAndCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, Pageable pageable);

    @Query(value = "SELECT m.* FROM memos m JOIN memo_tag_relations r ON m.id = r.memo_id " +
            "WHERE m.user_id = :userId AND r.tag_id = :tagId AND m.deleted_at IS NULL " +
            "ORDER BY m.pinned DESC, m.updated_at DESC",
            nativeQuery = true)
    List<Memo> findByUserIdAndTag(@Param("userId") UUID userId, @Param("tagId") UUID tagId, Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.userId = :userId AND m.id = :id AND m.deletedAt IS NULL")
    Optional<Memo> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT m FROM Memo m WHERE m.userId = :userId AND m.deletedAt IS NULL " +
            "ORDER BY m.pinned DESC, m.updatedAt DESC")
    List<Memo> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.userId = :userId AND m.deletedAt IS NULL " +
            "AND m.updatedAt < :cursor " +
            "ORDER BY m.pinned DESC, m.updatedAt DESC")
    List<Memo> findByUserIdAndCursor(@Param("userId") UUID userId, @Param("cursor") Instant cursor, Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.userId = :userId AND m.deletedAt IS NULL " +
            "AND m.remindAt IS NOT NULL AND m.remindAt <= :before " +
            "ORDER BY m.remindAt DESC")
    List<Memo> findByUserIdAndRemindBefore(@Param("userId") UUID userId, @Param("before") Instant before, Pageable pageable);

    @Query("SELECT count(m) FROM Memo m WHERE m.userId = :userId AND m.deletedAt IS NULL " +
            "AND m.status = 'active' AND m.remindAt IS NOT NULL AND m.remindAt >= CURRENT_TIMESTAMP")
    long countUpcomingReminders(@Param("userId") UUID userId);
}
