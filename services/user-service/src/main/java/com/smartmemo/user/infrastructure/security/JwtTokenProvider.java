package com.smartmemo.user.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * JWT Token 提供者。
 * 负责 Access Token 和 Refresh Token 的生成、解析和校验。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(JwtConfig config) {
        // 确保密钥至少 256 位
        byte[] keyBytes = config.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 不够长则用 SHA-256 拉伸到 256 位
            keyBytes = sha256(config.getSecret());
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = config.getAccessTokenExpiration();
        this.refreshTokenExpiration = config.getRefreshTokenExpiration();
    }

    // ==================== Access Token ====================

    /**
     * 生成 Access Token。
     */
    public String generateAccessToken(UUID userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpiration)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Access Token 中提取 userId。
     */
    public UUID getUserIdFromToken(String token) {
        String subject = parseClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    /**
     * 从 Access Token 中提取 username。
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /**
     * 校验 Access Token 是否有效。
     */
    public boolean validateAccessToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            // 确认是 access token
            String type = claims.getPayload().get("type", String.class);
            return "access".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid access token: {}", e.getMessage());
            return false;
        }
    }

    // ==================== Refresh Token ====================

    /**
     * 生成 Refresh Token（独立的 JWT）。
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenExpiration)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验 Refresh Token 是否有效。
     */
    public boolean validateRefreshToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            String type = claims.getPayload().get("type", String.class);
            return "refresh".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 计算 Refresh Token 的 SHA-256 哈希（用于数据库存储）。
     */
    public String hashToken(String token) {
        return sha256Hex(token);
    }

    /**
     * 获取 Refresh Token 的过期时间（秒）。
     */
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration;
    }

    // ==================== 内部方法 ====================

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String sha256Hex(String input) {
        return HexFormat.of().formatHex(sha256(input));
    }
}
