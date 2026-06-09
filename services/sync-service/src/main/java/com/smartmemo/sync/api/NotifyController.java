package com.smartmemo.sync.api;

import com.smartmemo.sync.application.SyncApplicationService;
import com.smartmemo.sync.websocket.SyncWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 内部通知端点：memo-service 在变更后调用，触发 WebSocket 推送。
 * 不走 Gateway 鉴权（内部调用），使用简单 header 传递 userId。
 */
@RestController
@RequestMapping("/internal")
public class NotifyController {

    private final SyncWebSocketHandler wsHandler;
    private final SyncApplicationService syncService;

    public NotifyController(SyncWebSocketHandler wsHandler, SyncApplicationService syncService) {
        this.wsHandler = wsHandler;
        this.syncService = syncService;
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> notify(
            @RequestHeader("X-User-Id") String userIdStr,
            @RequestBody Map<String, Object> body) {

        UUID userId = UUID.fromString(userIdStr);
        String type = (String) body.getOrDefault("type", "memo_changed");
        String memoId = (String) body.get("memoId");

        // 更新游标时间
        syncService.updateCursor(userId, java.time.Instant.now().toString());

        // 推送 WebSocket 通知
        String message = String.format(
                "{\"type\":\"%s\",\"memoId\":\"%s\",\"timestamp\":\"%s\"}",
                type, memoId, java.time.Instant.now()
        );
        wsHandler.sendToUser(userIdStr, message);

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
