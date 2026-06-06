package com.smartmemo.user.api;

import com.smartmemo.user.api.dto.UpdateUserRequest;
import com.smartmemo.user.api.dto.UserResponse;
import com.smartmemo.user.application.UserApplicationService;
import com.smartmemo.user.application.UserApplicationService.UserResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 用户信息接口。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userService;

    public UserController(UserApplicationService userService) {
        this.userService = userService;
    }

    /** 获取当前用户信息 */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        return userService.getCurrentUser(userId)
                .map(u -> ResponseEntity.ok(Map.of(
                        "code", "OK", "message", "success",
                        "data", toResponse(u),
                        "traceId", traceId())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", "USER_NOT_FOUND", "message", "用户不存在",
                                "data", null, "traceId", traceId())));
    }

    /** 更新当前用户信息 */
    @PatchMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMe(
            @RequestAttribute("userId") String userIdStr,
            @Valid @RequestBody UpdateUserRequest req) {
        UUID userId = UUID.fromString(userIdStr);
        return userService.updateUser(userId, req.email(), req.phone())
                .map(u -> ResponseEntity.ok(Map.of(
                        "code", "OK", "message", "success",
                        "data", toResponse(u),
                        "traceId", traceId())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", "USER_NOT_FOUND", "message", "用户不存在",
                                "data", null, "traceId", traceId())));
    }

    private static UserResponse toResponse(UserResult u) {
        return new UserResponse(u.id(), u.username(), u.email(), u.phone(),
                u.status().name(), u.createdAt());
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }
}
