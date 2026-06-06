package com.smartmemo.memo.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 备忘录-标签关联（纯 JDBC，不用 JPA Entity）。
 */
@Repository
public class MemoTagRelationRepository {

    private final JdbcTemplate jdbc;

    public MemoTagRelationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void addRelation(UUID memoId, UUID tagId) {
        jdbc.update("INSERT INTO memo_tag_relations (memo_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                memoId, tagId);
    }

    public void removeRelation(UUID memoId, UUID tagId) {
        jdbc.update("DELETE FROM memo_tag_relations WHERE memo_id = ? AND tag_id = ?", memoId, tagId);
    }

    public void replaceRelations(UUID memoId, List<UUID> tagIds) {
        jdbc.update("DELETE FROM memo_tag_relations WHERE memo_id = ?", memoId);
        for (UUID tagId : tagIds) {
            jdbc.update("INSERT INTO memo_tag_relations (memo_id, tag_id) VALUES (?, ?)", memoId, tagId);
        }
    }

    public List<UUID> findTagIdsByMemoId(UUID memoId) {
        return jdbc.queryForList("SELECT tag_id FROM memo_tag_relations WHERE memo_id = ?", UUID.class, memoId);
    }
}
