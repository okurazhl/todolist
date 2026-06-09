package com.smartmemo.sync.api;

import com.smartmemo.sync.api.dto.PushRequest;
import com.smartmemo.sync.application.SyncApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncApplicationService syncService;

    public SyncController(SyncApplicationService syncService) {
        this.syncService = syncService;
    }

    /** 从 Gateway 注入的 attribute 或内部调用的 header 获取 userId */
    private static UUID getUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (attr != null) return UUID.fromString(attr.toString());
        String header = request.getHeader("X-User-Id");
        if (header != null) return UUID.fromString(header);
        throw new IllegalArgumentException("缺少 userId");
    }

    /**
     * 增量拉取变更。
     * cursor 为 null 时全量拉取。
     */
    @GetMapping("/pull")
    public ResponseEntity<Map<String, Object>> pull(
            HttpServletRequest request,
            @RequestParam(required = false) String cursor) {
        UUID userId = getUserId(request);
        var result = syncService.pull(userId, cursor);
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success",
                "data", result, "traceId", traceId()
        ));
    }

    /**
     * 推送本地变更（版本冲突检测）。
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> push(
            HttpServletRequest request,
            @RequestBody PushRequest body) {
        UUID userId = getUserId(request);
        var result = syncService.push(userId, body);
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success",
                "data", result, "traceId", traceId()
        ));
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }
}
