package com.smartmemo.memo.api;

import com.smartmemo.memo.api.dto.*;
import com.smartmemo.memo.application.MemoApplicationService;
import com.smartmemo.memo.application.MemoApplicationService.MemoResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memos")
public class MemoController {

    private final MemoApplicationService memoService;

    public MemoController(MemoApplicationService memoService) {
        this.memoService = memoService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestAttribute("userId") String userIdStr,
                                                       @Valid @RequestBody CreateMemoRequest req) {
        UUID userId = UUID.fromString(userIdStr);
        MemoResult result = memoService.create(userId, req.title(), req.content(),
                req.categoryId(), req.tagIds(), req.isPinned(), req.remindAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("code", "OK", "message", "success", "data", toResponse(result), "traceId", traceId()));
    }

    @GetMapping("/{memoId}")
    public ResponseEntity<Map<String, Object>> get(@RequestAttribute("userId") String userIdStr,
                                                    @PathVariable UUID memoId) {
        UUID userId = UUID.fromString(userIdStr);
        return memoService.getById(memoId, userId)
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", "MEMO_NOT_FOUND", "message", "备忘录不存在", "data", "", "traceId", traceId())));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestAttribute("userId") String userIdStr,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) UUID categoryId,
                                                     @RequestParam(required = false) UUID tagId,
                                                     @RequestParam(required = false) String cursor,
                                                     @RequestParam(required = false) String remindBefore,
                                                     @RequestParam(defaultValue = "20") int limit) {
        UUID userId = UUID.fromString(userIdStr);
        var result = memoService.list(userId, status, categoryId, tagId, cursor, remindBefore, Math.min(limit, 50));
        List<MemoResponse> items = result.items().stream().map(MemoController::toResponse).toList();
        return ResponseEntity.ok(Map.of("code", "OK", "message", "success",
                "data", new MemoListResponse(items, result.nextCursor(), result.hasMore()), "traceId", traceId()));
    }

    @PatchMapping("/{memoId}")
    public ResponseEntity<Map<String, Object>> update(@RequestAttribute("userId") String userIdStr,
                                                       @PathVariable UUID memoId,
                                                       @Valid @RequestBody UpdateMemoRequest req) {
        UUID userId = UUID.fromString(userIdStr);
        return memoService.update(memoId, userId, req.title(), req.content(), req.categoryId(),
                        req.tagIds(), req.isPinned())
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", "MEMO_NOT_FOUND", "message", "备忘录不存在", "data", "", "traceId", traceId())));
    }

    @DeleteMapping("/{memoId}")
    public ResponseEntity<Map<String, Object>> delete(@RequestAttribute("userId") String userIdStr,
                                                       @PathVariable UUID memoId) {
        UUID userId = UUID.fromString(userIdStr);
        return memoService.delete(memoId, userId)
                ? ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", "", "traceId", traceId()))
                : ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", "MEMO_NOT_FOUND", "message", "备忘录不存在", "data", "", "traceId", traceId()));
    }

    @PostMapping("/{memoId}/pin")
    public ResponseEntity<Map<String, Object>> pin(@RequestAttribute("userId") String userIdStr,
                                                    @PathVariable UUID memoId) {
        return memoService.pin(memoId, UUID.fromString(userIdStr), true)
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(notFound());
    }

    @DeleteMapping("/{memoId}/pin")
    public ResponseEntity<Map<String, Object>> unpin(@RequestAttribute("userId") String userIdStr,
                                                      @PathVariable UUID memoId) {
        return memoService.pin(memoId, UUID.fromString(userIdStr), false)
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(notFound());
    }

    @PostMapping("/{memoId}/archive")
    public ResponseEntity<Map<String, Object>> archive(@RequestAttribute("userId") String userIdStr,
                                                        @PathVariable UUID memoId) {
        return memoService.archive(memoId, UUID.fromString(userIdStr), true)
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(notFound());
    }

    @DeleteMapping("/{memoId}/archive")
    public ResponseEntity<Map<String, Object>> unarchive(@RequestAttribute("userId") String userIdStr,
                                                          @PathVariable UUID memoId) {
        return memoService.archive(memoId, UUID.fromString(userIdStr), false)
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(notFound());
    }

    @PostMapping("/{memoId}/complete")
    public ResponseEntity<Map<String, Object>> complete(@RequestAttribute("userId") String userIdStr,
                                                         @PathVariable UUID memoId) {
        return memoService.complete(memoId, UUID.fromString(userIdStr), true)
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(notFound());
    }

    @DeleteMapping("/{memoId}/complete")
    public ResponseEntity<Map<String, Object>> uncomplete(@RequestAttribute("userId") String userIdStr,
                                                           @PathVariable UUID memoId) {
        return memoService.complete(memoId, UUID.fromString(userIdStr), false)
                .map(r -> ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", toResponse(r), "traceId", traceId())))
                .orElse(notFound());
    }

    @GetMapping("/reminder-count")
    public ResponseEntity<Map<String, Object>> reminderCount(@RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        long count = memoService.countReminders(userId);
        return ResponseEntity.ok(Map.of("code", "OK", "message", "success",
                "data", Map.of("count", count), "traceId", traceId()));
    }

    private static MemoResponse toResponse(MemoResult r) {
        return new MemoResponse(r.id(), r.title(), r.content(), r.categoryId(),
                r.status(), r.pinned(), r.tagIds(), r.remindAt(), r.createdAt(), r.updatedAt());
    }

    private ResponseEntity<Map<String, Object>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "MEMO_NOT_FOUND", "message", "备忘录不存在", "data", "", "traceId", traceId()));
    }

    private static String traceId() { return UUID.randomUUID().toString(); }
}
