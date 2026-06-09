package com.smartmemo.memo.infrastructure.persistence;

import com.smartmemo.memo.domain.MemoAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoAttachmentRepository extends JpaRepository<MemoAttachment, UUID> {

    @Query("SELECT a FROM MemoAttachment a WHERE a.memoId = :memoId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    List<MemoAttachment> findByMemoId(@Param("memoId") UUID memoId);

    @Query("SELECT a FROM MemoAttachment a WHERE a.id = :id AND a.userId = :userId AND a.deletedAt IS NULL")
    Optional<MemoAttachment> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
