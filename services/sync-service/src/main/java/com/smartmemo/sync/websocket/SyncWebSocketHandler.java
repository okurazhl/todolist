package com.smartmemo.sync.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 处理器：管理客户端连接，支持向特定用户推送同步通知。
 */
@Component
public class SyncWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SyncWebSocketHandler.class);

    // userId → sessions 映射
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("WS connected: userId={}, sessionId={}", userId, session.getId());
        } else {
            log.warn("WS connection without userId, closing: {}", session.getId());
            try { session.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) userSessions.remove(userId);
            }
        }
        log.info("WS disconnected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 心跳回复
        if ("ping".equals(message.getPayload())) {
            try { session.sendMessage(new TextMessage("pong")); } catch (IOException ignored) {}
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WS transport error: sessionId={}", session.getId(), exception);
    }

    /**
     * 向指定用户的所有会话推送消息。
     */
    public void sendToUser(String userId, String message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage msg = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(msg);
                } catch (IOException e) {
                    log.warn("WS send failed: userId={}, sessionId={}", userId, session.getId());
                }
            }
        }
    }

    /**
     * 从 WebSocket 握手 URL 的查询参数中提取 userId。
     * 客户端连接 ws://host:port/ws?token=<jwt>
     * Gateway 会将 JWT 解析后的 userId 注入，或我们本地解析。
     */
    private String getUserId(WebSocketSession session) {
        // 尝试从 query params 获取
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "token".equals(kv[0]) && !kv[1].isEmpty()) {
                    // 简单处理：从 JWT 中提取 userId（base64 decode payload）
                    return extractUserIdFromToken(kv[1]);
                }
            }
        }
        return null;
    }

    /**
     * 从 JWT token 的 payload 中提取 userId (sub claim)。
     * 生产环境应使用 JWT 库验证签名。
     */
    private String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // 简单 JSON 提取 sub
            int start = payload.indexOf("\"sub\"");
            if (start < 0) return null;
            start = payload.indexOf("\"", start + 5) + 1;
            int end = payload.indexOf("\"", start);
            if (start > 0 && end > start) return payload.substring(start, end);
        } catch (Exception ignored) {}
        return null;
    }
}
