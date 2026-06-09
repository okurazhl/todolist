package com.smartmemo.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * JWT 鉴权全局过滤器。
 * 校验请求中的 Bearer Token，将 userId/username 注入请求头传递给下游服务。
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** 无需鉴权的路径 */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/health",
            "/actuator"
    );

    private final SecretKey secretKey;

    public AuthFilter(@Value("${jwt.secret:smartmemo-dev-secret-key-change-in-production-min-256-bits}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = sha256(secret);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 公开路径直接放行
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 提取 Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeError(exchange, "AUTH_UNAUTHORIZED", "缺少认证 Token", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 校验 Token
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                return writeError(exchange, "AUTH_TOKEN_INVALID", "Token 类型不正确", HttpStatus.UNAUTHORIZED);
            }

            // 注入用户信息到请求头，下游服务直接读取
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);

            var request = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username)
                    .build();

            exchange = exchange.mutate()
                    .request(request)
                    .build();

            exchange.getAttributes().put("userId", userId);
            exchange.getAttributes().put("username", username);

        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return writeError(exchange, "AUTH_TOKEN_INVALID", "Token 无效或已过期", HttpStatus.UNAUTHORIZED);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, String code, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"data\":null,\"traceId\":\"%s\"}",
                code, message, java.util.UUID.randomUUID());
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
