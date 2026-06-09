package com.smartmemo.memo.infrastructure.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * 备忘录变更后通知 sync-service，触发 WebSocket 推送。
 */
@Component
public class SyncNotifier {

    private static final Logger log = LoggerFactory.getLogger(SyncNotifier.class);

    private final RestTemplate restTemplate;
    private final String syncServiceUrl;

    public SyncNotifier(@Value("${sync.service.url:http://localhost:8083}") String syncServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.syncServiceUrl = syncServiceUrl;
    }

    public void notifyChanged(UUID userId, UUID memoId, String type) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", userId.toString());

            Map<String, Object> body = Map.of(
                    "type", type,
                    "memoId", memoId.toString()
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(syncServiceUrl + "/internal/notify", request, String.class);

            log.debug("Sync notified: userId={}, memoId={}, type={}", userId, memoId, type);
        } catch (Exception e) {
            // 通知失败不影响主流程
            log.warn("Failed to notify sync-service: {}", e.getMessage());
        }
    }
}
