package com.smartmemo.memo.api;

import com.smartmemo.memo.api.dto.CreateTagRequest;
import com.smartmemo.memo.api.dto.TagResponse;
import com.smartmemo.memo.application.TagApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagApplicationService tagService;

    public TagController(TagApplicationService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        List<TagResponse> tags = tagService.list(userId).stream()
                .map(t -> new TagResponse(t.id(), t.name(), t.color())).toList();
        return ResponseEntity.ok(Map.of("code", "OK", "message", "success",
                "data", Map.of("items", tags), "traceId", traceId()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestAttribute("userId") String userIdStr,
                                                       @Valid @RequestBody CreateTagRequest req) {
        try {
            var result = tagService.create(UUID.fromString(userIdStr), req.name(), req.color());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("code", "OK", "message", "success",
                            "data", new TagResponse(result.id(), result.name(), result.color()), "traceId", traceId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "TAG_ALREADY_EXISTS", "message", "标签已存在", "data", null, "traceId", traceId()));
        }
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Map<String, Object>> delete(@RequestAttribute("userId") String userIdStr,
                                                       @PathVariable UUID tagId) {
        return tagService.delete(tagId, UUID.fromString(userIdStr))
                ? ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", null, "traceId", traceId()))
                : ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", "MEMO_NOT_FOUND", "message", "标签不存在", "data", null, "traceId", traceId()));
    }

    private static String traceId() { return UUID.randomUUID().toString(); }
}
