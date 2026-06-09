package com.smartmemo.push.infrastructure;

import com.smartmemo.push.domain.PushMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PushMessageRepository extends JpaRepository<PushMessage, UUID> {

    List<PushMessage> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    @Query("SELECT p FROM PushMessage p WHERE p.userId = :userId AND p.read = false ORDER BY p.createdAt DESC")
    List<PushMessage> findUnreadByUserId(@Param("userId") UUID userId);

    boolean existsByMemoIdAndType(UUID memoId, String type);
}
