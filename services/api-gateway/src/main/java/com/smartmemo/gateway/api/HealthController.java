package com.smartmemo.gateway.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 健康检查接口。
 * 用于探活和验证统一响应格式。
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of(
                "code", "OK",
                "message", "success",
                "data", Map.of(
                        "service", "api-gateway",
                        "status", "UP",
                        "timestamp", Instant.now().toString()
                ),
                "traceId", java.util.UUID.randomUUID().toString()
        );
    }
}
