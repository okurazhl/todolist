package com.smartmemo.memo.application;

import com.smartmemo.memo.domain.Category;
import com.smartmemo.memo.infrastructure.persistence.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CategoryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CategoryApplicationService.class);

    private final CategoryRepository categoryRepository;

    public CategoryApplicationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResult> list(UUID userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(CategoryResult::from)
                .toList();
    }

    @Transactional
    public CategoryResult create(UUID userId, String name, String color, int sortOrder) {
        Optional<Category> existing = categoryRepository.findByUserIdAndName(userId, name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("CATEGORY_ALREADY_EXISTS:分类已存在");
        }
        Category cat = new Category();
        cat.setUserId(userId);
        cat.setName(name);
        cat.setColor(color);
        cat.setSortOrder(sortOrder);
        Category saved = categoryRepository.save(cat);
        log.info("Category created: id={}, name={}", saved.getId(), name);
        return CategoryResult.from(saved);
    }

    @Transactional
    public boolean delete(UUID categoryId, UUID userId) {
        Optional<Category> opt = categoryRepository.findById(categoryId);
        if (opt.isEmpty() || !opt.get().getUserId().equals(userId)) return false;
        Category cat = opt.get();
        cat.setDeletedAt(Instant.now());
        categoryRepository.save(cat);
        return true;
    }

    public record CategoryResult(UUID id, String name, String color, int sortOrder) {
        public static CategoryResult from(Category c) {
            return new CategoryResult(c.getId(), c.getName(), c.getColor(), c.getSortOrder());
        }
    }
}
