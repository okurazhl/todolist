package com.smartmemo.gateway.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 全局异常处理。
 * 统一将异常转换为标准错误响应格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception: traceId={}", traceId, ex);

        Map<String, Object> body = Map.of(
                "code", "SYS_ERROR",
                "message", ex.getMessage() != null ? ex.getMessage() : "Internal server error",
                "data", null,
                "traceId", traceId,
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
