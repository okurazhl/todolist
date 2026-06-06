package com.smartmemo.user.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-for-jwt-unit-tests-min-256-bits!!!!!!!!!");
        config.setAccessTokenExpiration(60);   // 1 分钟
        config.setRefreshTokenExpiration(600); // 10 分钟
        provider = new JwtTokenProvider(config);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "testuser");

        assertNotNull(token);
        assertTrue(provider.validateAccessToken(token));
        assertEquals(userId, provider.getUserIdFromToken(token));
        assertEquals("testuser", provider.getUsernameFromToken(token));
    }

    @Test
    void shouldRejectExpiredAccessToken() throws Exception {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-for-jwt-unit-tests-min-256-bits!!!!!!!!!");
        config.setAccessTokenExpiration(-1); // 立即过期
        JwtTokenProvider shortProvider = new JwtTokenProvider(config);

        String token = shortProvider.generateAccessToken(UUID.randomUUID(), "testuser");
        // 等待 token 过期
        Thread.sleep(10);
        assertFalse(shortProvider.validateAccessToken(token));
    }

    @Test
    void shouldRejectRefreshTokenAsAccessToken() {
        String refreshToken = provider.generateRefreshToken(UUID.randomUUID());
        assertFalse(provider.validateAccessToken(refreshToken));
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {
        String token = provider.generateRefreshToken(UUID.randomUUID());
        assertNotNull(token);
        assertTrue(provider.validateRefreshToken(token));
    }

    @Test
    void shouldRejectAccessTokenAsRefreshToken() {
        String accessToken = provider.generateAccessToken(UUID.randomUUID(), "testuser");
        assertFalse(provider.validateRefreshToken(accessToken));
    }

    @Test
    void shouldProduceConsistentTokenHash() {
        String token = "some-refresh-token-value";
        String hash1 = provider.hashToken(token);
        String hash2 = provider.hashToken(token);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 hex = 64 chars
    }

    @Test
    void shouldRejectTokenWithWrongSecret() {
        String token = provider.generateAccessToken(UUID.randomUUID(), "testuser");

        JwtConfig otherConfig = new JwtConfig();
        otherConfig.setSecret("different-secret-key-for-testing-purposes-min-256!!");
        otherConfig.setAccessTokenExpiration(60);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherConfig);

        assertFalse(otherProvider.validateAccessToken(token));
    }
}
