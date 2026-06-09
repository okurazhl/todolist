package com.smartmemo.user.application;

import com.smartmemo.user.domain.RefreshToken;
import com.smartmemo.user.domain.User;
import com.smartmemo.user.domain.UserStatus;
import com.smartmemo.user.infrastructure.persistence.RefreshTokenRepository;
import com.smartmemo.user.infrastructure.persistence.UserDeviceRepository;
import com.smartmemo.user.infrastructure.persistence.UserRepository;
import com.smartmemo.user.infrastructure.redis.TokenBlacklistService;
import com.smartmemo.user.infrastructure.security.JwtConfig;
import com.smartmemo.user.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * AuthApplicationService 单元测试（Mock 依赖）。
 */
@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDeviceRepository deviceRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlacklistService tokenBlacklistService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private JwtTokenProvider jwtTokenProvider;
    private AuthApplicationService authService;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-for-jwt-unit-tests-min-256-bits!!!!!!!!!");
        config.setAccessTokenExpiration(60);
        config.setRefreshTokenExpiration(600);
        jwtTokenProvider = new JwtTokenProvider(config);

        authService = new AuthApplicationService(userRepository, deviceRepository,
                refreshTokenRepository, jwtTokenProvider, tokenBlacklistService, passwordEncoder);
    }

    @Test
    void shouldRegisterSuccessfully() {
        when(userRepository.existsByUsernameAndDeletedAtIsNull("newuser")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        var result = authService.register("newuser", "Password123", "new@example.com", null);

        assertTrue(result.success());
        assertEquals("newuser", result.username());
    }

    @Test
    void shouldFailOnDuplicateUsername() {
        when(userRepository.existsByUsernameAndDeletedAtIsNull("existing")).thenReturn(true);

        var result = authService.register("existing", "Password123", "a@b.com", null);

        assertFalse(result.success());
        assertEquals("USER_ALREADY_EXISTS", result.errorCode());
    }

    @Test
    void shouldFailOnWeakPassword() {
        var result = authService.register("user", "12345", null, null);
        assertFalse(result.success());
        assertEquals("USER_WEAK_PASSWORD", result.errorCode());
    }

    @Test
    void shouldLoginSuccessfully() {
        String rawPassword = "Password123";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(UserStatus.active);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authService.login("testuser", rawPassword, "web", "Chrome");

        assertTrue(result.success());
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
    }

    @Test
    void shouldFailOnWrongPassword() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode("CorrectPassword"));
        user.setStatus(UserStatus.active);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        var result = authService.login("testuser", "WrongPassword", null, null);

        assertFalse(result.success());
        assertEquals("AUTH_INVALID_CREDENTIALS", result.errorCode());
    }

    @Test
    void shouldFailOnDisabledUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("disabled");
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setStatus(UserStatus.disabled);

        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(user));

        var result = authService.login("disabled", "Password123", null, null);

        assertFalse(result.success());
        assertEquals("USER_DISABLED", result.errorCode());
    }

    @Test
    void shouldFailRefreshWithInvalidToken() {
        var result = authService.refresh("invalid-token");
        assertFalse(result.success());
        assertEquals("AUTH_TOKEN_INVALID", result.errorCode());
    }
}
