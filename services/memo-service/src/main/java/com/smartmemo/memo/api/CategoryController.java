package com.smartmemo.memo.api;

import com.smartmemo.memo.api.dto.CategoryResponse;
import com.smartmemo.memo.api.dto.CreateCategoryRequest;
import com.smartmemo.memo.application.CategoryApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryApplicationService categoryService;

    public CategoryController(CategoryApplicationService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        List<CategoryResponse> items = categoryService.list(userId).stream()
                .map(c -> new CategoryResponse(c.id(), c.name(), c.color(), c.sortOrder())).toList();
        return ResponseEntity.ok(Map.of("code", "OK", "message", "success",
                "data", Map.of("items", items), "traceId", traceId()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestAttribute("userId") String userIdStr,
                                                       @Valid @RequestBody CreateCategoryRequest req) {
        try {
            var result = categoryService.create(UUID.fromString(userIdStr), req.name(), req.color(), req.sortOrder());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("code", "OK", "message", "success",
                            "data", new CategoryResponse(result.id(), result.name(), result.color(), result.sortOrder()),
                            "traceId", traceId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "CATEGORY_ALREADY_EXISTS", "message", "分类已存在", "data", null, "traceId", traceId()));
        }
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Map<String, Object>> delete(@RequestAttribute("userId") String userIdStr,
                                                       @PathVariable UUID categoryId) {
        return categoryService.delete(categoryId, UUID.fromString(userIdStr))
                ? ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", null, "traceId", traceId()))
                : notFound();
    }

    private ResponseEntity<Map<String, Object>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "MEMO_NOT_FOUND", "message", "分类不存在", "data", null, "traceId", traceId()));
    }

    private static String traceId() { return UUID.randomUUID().toString(); }
}
