package com.smartmemo.sync.infrastructure.persistence;

import com.smartmemo.sync.domain.SyncMemo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SyncMemoRepository extends JpaRepository<SyncMemo, UUID> {

    /**
     * 查询用户自 cursor 以来的所有变更（包括软删除）。
     * cursor 是 ISO-8601 时间戳，对应 updated_at。
     */
    @Query("SELECT m FROM SyncMemo m WHERE m.userId = :userId AND m.updatedAt > :since ORDER BY m.updatedAt ASC")
    List<SyncMemo> findChangedSince(@Param("userId") UUID userId, @Param("since") Instant since, Pageable pageable);

    /**
     * 查询用户所有活跃备忘录（初始化同步用）。
     */
    @Query("SELECT m FROM SyncMemo m WHERE m.userId = :userId AND m.deletedAt IS NULL ORDER BY m.updatedAt ASC")
    List<SyncMemo> findAllActive(@Param("userId") UUID userId, Pageable pageable);
}
