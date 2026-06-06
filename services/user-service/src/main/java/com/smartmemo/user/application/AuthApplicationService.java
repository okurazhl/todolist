package com.smartmemo.user.application;

import com.smartmemo.user.domain.RefreshToken;
import com.smartmemo.user.domain.User;
import com.smartmemo.user.domain.UserDevice;
import com.smartmemo.user.domain.UserStatus;
import com.smartmemo.user.infrastructure.persistence.RefreshTokenRepository;
import com.smartmemo.user.infrastructure.persistence.UserDeviceRepository;
import com.smartmemo.user.infrastructure.persistence.UserRepository;
import com.smartmemo.user.infrastructure.redis.TokenBlacklistService;
import com.smartmemo.user.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * 认证应用服务。
 * 事务边界在此层。
 */
@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final UserRepository userRepository;
    private final UserDeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;

    public AuthApplicationService(UserRepository userRepository,
                                  UserDeviceRepository deviceRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  JwtTokenProvider jwtTokenProvider,
                                  TokenBlacklistService tokenBlacklistService,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户注册。
     */
    @Transactional
    public RegisterResult register(String username, String rawPassword, String email, String phone) {
        // 校验用户名唯一
        if (userRepository.existsByUsernameAndDeletedAtIsNull(username)) {
            return RegisterResult.failure("USER_ALREADY_EXISTS", "用户名已存在");
        }
        // 校验邮箱唯一
        if (email != null && userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            return RegisterResult.failure("USER_ALREADY_EXISTS", "邮箱已被注册");
        }
        // 校验密码强度
        if (rawPassword == null || rawPassword.length() < 8) {
            return RegisterResult.failure("USER_WEAK_PASSWORD", "密码至少 8 位");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(UserStatus.active);

        User saved = userRepository.save(user);
        log.info("User registered: userId={}, username={}", saved.getId(), saved.getUsername());

        return RegisterResult.success(saved.getId(), saved.getUsername());
    }

    /**
     * 用户登录，返回 Token 对。
     */
    @Transactional
    public LoginResult login(String username, String rawPassword, String deviceType, String deviceName) {
        // 查找用户
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return LoginResult.failure("AUTH_INVALID_CREDENTIALS", "用户名或密码错误");
        }
        if (user.getStatus() != UserStatus.active) {
            return LoginResult.failure("USER_DISABLED", "账号已被禁用");
        }
        // 校验密码
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return LoginResult.failure("AUTH_INVALID_CREDENTIALS", "用户名或密码错误");
        }

        // 记录设备
        UUID deviceId = null;
        if (deviceType != null) {
            UserDevice device = new UserDevice();
            device.setUserId(user.getId());
            device.setDeviceType(deviceType);
            device.setDeviceName(deviceName);
            device.setLastOnlineAt(Instant.now());
            UserDevice saved = deviceRepository.save(device);
            deviceId = saved.getId();
        }

        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 存储 Refresh Token 哈希
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(jwtTokenProvider.hashToken(refreshToken));
        rt.setDeviceId(deviceId);
        rt.setExpiresAt(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds()));
        refreshTokenRepository.save(rt);

        log.info("User logged in: userId={}, username={}", user.getId(), user.getUsername());
        return LoginResult.success(accessToken, refreshToken, jwtTokenProvider.getRefreshTokenExpirationSeconds());
    }

    /**
     * 刷新 Token 对（轮换）。
     */
    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        // 校验 Token 格式和签名
        if (!jwtTokenProvider.validateRefreshToken(rawRefreshToken)) {
            return LoginResult.failure("AUTH_TOKEN_INVALID", "Refresh Token 无效或已过期");
        }

        // 查找数据库记录
        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (rt == null || rt.isRevoked()) {
            // 可能被轮换攻击，撤销该用户所有 refresh token
            log.warn("Refresh token reuse detected: hash={}", tokenHash);
            return LoginResult.failure("AUTH_TOKEN_INVALID", "Refresh Token 已被撤销");
        }

        // 撤销旧的 Refresh Token
        rt.setRevokedAt(Instant.now());
        refreshTokenRepository.save(rt);

        // 获取用户信息
        User user = userRepository.findById(rt.getUserId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.active) {
            return LoginResult.failure("USER_DISABLED", "账号不可用");
        }

        // 生成新的 Token 对
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshToken newRt = new RefreshToken();
        newRt.setUserId(user.getId());
        newRt.setTokenHash(jwtTokenProvider.hashToken(newRefreshToken));
        newRt.setDeviceId(rt.getDeviceId());
        newRt.setExpiresAt(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds()));
        refreshTokenRepository.save(newRt);

        log.info("Token refreshed: userId={}", user.getId());
        return LoginResult.success(newAccessToken, newRefreshToken, jwtTokenProvider.getRefreshTokenExpirationSeconds());
    }

    /**
     * 登出。
     */
    @Transactional
    public void logout(String accessToken, UUID userId) {
        // Access Token 加入黑名单
        long remainingSeconds = jwtTokenProvider.getRefreshTokenExpirationSeconds();
        tokenBlacklistService.blacklist(accessToken, remainingSeconds);

        // 撤销该用户所有 Refresh Token
        refreshTokenRepository.revokeAllByUserId(userId);

        log.info("User logged out: userId={}", userId);
    }

    // ==================== 内部 Result 类型 ====================

    public record RegisterResult(boolean success, String errorCode, String errorMessage,
                                  UUID userId, String username) {
        public static RegisterResult success(UUID userId, String username) {
            return new RegisterResult(true, null, null, userId, username);
        }
        public static RegisterResult failure(String code, String message) {
            return new RegisterResult(false, code, message, null, null);
        }
    }

    public record LoginResult(boolean success, String errorCode, String errorMessage,
                               String accessToken, String refreshToken, long expiresIn) {
        public static LoginResult success(String accessToken, String refreshToken, long expiresIn) {
            return new LoginResult(true, null, null, accessToken, refreshToken, expiresIn);
        }
        public static LoginResult failure(String code, String message) {
            return new LoginResult(false, code, message, null, null, 0);
        }
    }
}
