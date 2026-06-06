package com.smartmemo.user.api;

import com.smartmemo.user.api.dto.*;
import com.smartmemo.user.application.AuthApplicationService;
import com.smartmemo.user.application.AuthApplicationService.LoginResult;
import com.smartmemo.user.application.AuthApplicationService.RegisterResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 认证接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthApplicationService authService;

    public AuthController(AuthApplicationService authService) {
        this.authService = authService;
    }

    /** 注册 */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        RegisterResult result = authService.register(req.username(), req.password(), req.email(), req.phone());
        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", result.errorCode(), "message", result.errorMessage(),
                            "data", null, "traceId", traceId()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("code", "OK", "message", "success",
                        "data", Map.of("userId", result.userId(), "username", result.username()),
                        "traceId", traceId()));
    }

    /** 登录 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        LoginResult result = authService.login(req.username(), req.password(), req.deviceType(), req.deviceName());
        if (!result.success()) {
            HttpStatus status = "AUTH_INVALID_CREDENTIALS".equals(result.errorCode())
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(status)
                    .body(Map.of("code", result.errorCode(), "message", result.errorMessage(),
                            "data", null, "traceId", traceId()));
        }
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success",
                "data", new AuthResponse(result.accessToken(), result.refreshToken(), result.expiresIn()),
                "traceId", traceId()));
    }

    /** 刷新 Token */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest req) {
        LoginResult result = authService.refresh(req.refreshToken());
        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", result.errorCode(), "message", result.errorMessage(),
                            "data", null, "traceId", traceId()));
        }
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success",
                "data", new AuthResponse(result.accessToken(), result.refreshToken(), result.expiresIn()),
                "traceId", traceId()));
    }

    /** 登出 */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestAttribute("userId") String userIdStr) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", null, "traceId", traceId()));
        }
        String token = authHeader.substring(7);
        authService.logout(token, UUID.fromString(userIdStr));
        return ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", null, "traceId", traceId()));
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }
}
