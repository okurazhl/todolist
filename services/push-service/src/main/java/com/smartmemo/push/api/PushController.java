package com.smartmemo.push.api;

import com.smartmemo.push.application.PushApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/push")
public class PushController {

    private final PushApplicationService pushService;

    public PushController(PushApplicationService pushService) {
        this.pushService = pushService;
    }

    private static UUID getUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr != null) return UUID.fromString(attr.toString());
        String header = request.getHeader("X-User-Id");
        if (header != null) return UUID.fromString(header);
        throw new IllegalArgumentException("缺少 userId");
    }

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> list(HttpServletRequest request) {
        UUID userId = getUserId(request);
        var messages = pushService.list(userId);
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success",
                "data", Map.of("items", messages, "unreadCount", pushService.countUnread(userId)),
                "traceId", traceId()
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> unreadCount(HttpServletRequest request) {
        UUID userId = getUserId(request);
        long count = pushService.countUnread(userId);
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success",
                "data", Map.of("count", count), "traceId", traceId()
        ));
    }

    @PostMapping("/messages/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            HttpServletRequest request, @PathVariable UUID id) {
        UUID userId = getUserId(request);
        pushService.markRead(id, userId);
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success", "data", "", "traceId", traceId()
        ));
    }

    @PostMapping("/messages/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(HttpServletRequest request) {
        UUID userId = getUserId(request);
        pushService.markAllRead(userId);
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success", "data", "", "traceId", traceId()
        ));
    }

    private static String traceId() { return UUID.randomUUID().toString(); }
}
