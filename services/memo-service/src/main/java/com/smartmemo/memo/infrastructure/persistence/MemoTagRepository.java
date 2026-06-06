package com.smartmemo.memo.infrastructure.persistence;

import com.smartmemo.memo.domain.MemoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoTagRepository extends JpaRepository<MemoTag, UUID> {

    @Query("SELECT t FROM MemoTag t WHERE t.userId = :userId AND t.deletedAt IS NULL ORDER BY t.name")
    List<MemoTag> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM MemoTag t WHERE t.userId = :userId AND t.name = :name AND t.deletedAt IS NULL")
    Optional<MemoTag> findByUserIdAndName(@Param("userId") UUID userId, @Param("name") String name);
}
