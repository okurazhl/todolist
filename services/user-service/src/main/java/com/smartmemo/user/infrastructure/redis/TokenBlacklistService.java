package com.smartmemo.user.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Token 黑名单服务。
 * 登出或刷新时将 Token 加入 Redis 黑名单，TTL 等于 Token 剩余有效期。
 * Key 格式: blacklist:{tokenHash}
 */
@Component
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将 Access Token 加入黑名单。
     * @param token 原始 token
     * @param ttlSeconds 黑名单有效期（应等于 token 剩余有效期）
     */
    public void blacklist(String token, long ttlSeconds) {
        String hash = sha256Hex(token);
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + hash, "1", Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 检查 Token 是否在黑名单中。
     */
    public boolean isBlacklisted(String token) {
        String hash = sha256Hex(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + hash));
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
