package com.smartmemo.memo.application;

import com.smartmemo.memo.domain.MemoTag;
import com.smartmemo.memo.infrastructure.persistence.MemoTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TagApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TagApplicationService.class);

    private final MemoTagRepository tagRepository;

    public TagApplicationService(MemoTagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagResult> list(UUID userId) {
        return tagRepository.findByUserId(userId).stream()
                .map(TagResult::from)
                .toList();
    }

    @Transactional
    public TagResult create(UUID userId, String name, String color) {
        Optional<MemoTag> existing = tagRepository.findByUserIdAndName(userId, name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("TAG_ALREADY_EXISTS:标签已存在");
        }
        MemoTag tag = new MemoTag();
        tag.setUserId(userId);
        tag.setName(name);
        tag.setColor(color);
        MemoTag saved = tagRepository.save(tag);
        log.info("Tag created: id={}, name={}", saved.getId(), name);
        return TagResult.from(saved);
    }

    @Transactional
    public boolean delete(UUID tagId, UUID userId) {
        Optional<MemoTag> opt = tagRepository.findById(tagId);
        if (opt.isEmpty() || !opt.get().getUserId().equals(userId)) return false;
        MemoTag tag = opt.get();
        tag.setDeletedAt(Instant.now());
        tagRepository.save(tag);
        return true;
    }

    public record TagResult(UUID id, String name, String color) {
        public static TagResult from(MemoTag t) {
            return new TagResult(t.getId(), t.getName(), t.getColor());
        }
    }
}
