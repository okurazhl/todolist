package com.smartmemo.memo.api;

import com.smartmemo.memo.api.dto.AttachmentResponse;
import com.smartmemo.memo.application.AttachmentApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class AttachmentController {

    private final AttachmentApplicationService attachmentService;

    public AttachmentController(AttachmentApplicationService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/api/v1/memos/{memoId}/attachments")
    public ResponseEntity<Map<String, Object>> upload(@RequestAttribute("userId") String userIdStr,
                                                       @PathVariable UUID memoId,
                                                       @RequestParam("file") MultipartFile file) throws IOException {
        UUID userId = UUID.fromString(userIdStr);
        var result = attachmentService.upload(memoId, userId, file.getOriginalFilename(),
                file.getSize(), file.getContentType(), file.getInputStream());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("code", "OK", "message", "success",
                        "data", toResponse(result), "traceId", traceId()));
    }

    @GetMapping("/api/v1/memos/{memoId}/attachments")
    public ResponseEntity<Map<String, Object>> list(@RequestAttribute("userId") String userIdStr,
                                                     @PathVariable UUID memoId) {
        List<AttachmentResponse> items = attachmentService.listByMemo(memoId).stream()
                .map(AttachmentController::toResponse).toList();
        return ResponseEntity.ok(Map.of("code", "OK", "message", "success",
                "data", Map.of("items", items), "traceId", traceId()));
    }

    @GetMapping("/api/v1/attachments/{attachmentId}/download")
    public ResponseEntity<Map<String, Object>> download(@RequestAttribute("userId") String userIdStr,
                                                         @PathVariable UUID attachmentId) {
        UUID userId = UUID.fromString(userIdStr);
        return attachmentService.getDownloadUrl(attachmentId, userId)
                .map(url -> ResponseEntity.ok(Map.of("code", "OK", "message", "success",
                        "data", Map.of("downloadUrl", url), "traceId", traceId())))
                .orElse(notFound());
    }

    @DeleteMapping("/api/v1/attachments/{attachmentId}")
    public ResponseEntity<Map<String, Object>> delete(@RequestAttribute("userId") String userIdStr,
                                                       @PathVariable UUID attachmentId) {
        return attachmentService.delete(attachmentId, UUID.fromString(userIdStr))
                ? ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", null, "traceId", traceId()))
                : notFound();
    }

    private static AttachmentResponse toResponse(AttachmentApplicationService.AttachmentResult r) {
        return new AttachmentResponse(r.id(), r.memoId(), r.fileName(), r.fileSize(), r.contentType(), r.createdAt());
    }

    private ResponseEntity<Map<String, Object>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "FILE_NOT_FOUND", "message", "附件不存在", "data", null, "traceId", traceId()));
    }

    private static String traceId() { return UUID.randomUUID().toString(); }
}
