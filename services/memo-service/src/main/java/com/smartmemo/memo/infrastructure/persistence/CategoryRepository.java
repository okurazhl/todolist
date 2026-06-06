package com.smartmemo.memo.infrastructure.persistence;

import com.smartmemo.memo.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.deletedAt IS NULL ORDER BY c.sortOrder, c.name")
    List<Category> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.name = :name AND c.deletedAt IS NULL")
    Optional<Category> findByUserIdAndName(@Param("userId") UUID userId, @Param("name") String name);
}
