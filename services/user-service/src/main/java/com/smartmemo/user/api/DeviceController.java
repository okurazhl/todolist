package com.smartmemo.user.api;

import com.smartmemo.user.api.dto.DeviceRequest;
import com.smartmemo.user.api.dto.DeviceResponse;
import com.smartmemo.user.application.DeviceApplicationService;
import com.smartmemo.user.application.DeviceApplicationService.DeviceResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 设备管理接口。
 */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceApplicationService deviceService;

    public DeviceController(DeviceApplicationService deviceService) {
        this.deviceService = deviceService;
    }

    /** 获取设备列表 */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestAttribute("userId") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        List<DeviceResponse> devices = deviceService.listDevices(userId).stream()
                .map(DeviceController::toResponse)
                .toList();
        return ResponseEntity.ok(Map.of(
                "code", "OK", "message", "success",
                "data", Map.of("items", devices),
                "traceId", traceId()));
    }

    /** 绑定设备 */
    @PostMapping
    public ResponseEntity<Map<String, Object>> bind(
            @RequestAttribute("userId") String userIdStr,
            @Valid @RequestBody DeviceRequest req) {
        UUID userId = UUID.fromString(userIdStr);
        DeviceResult result = deviceService.bindDevice(userId, req.deviceType(),
                req.deviceName(), req.pushToken(), req.pushProvider());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("code", "OK", "message", "success",
                        "data", toResponse(result),
                        "traceId", traceId()));
    }

    /** 解绑设备 */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> unbind(
            @RequestAttribute("userId") String userIdStr,
            @PathVariable UUID deviceId) {
        UUID userId = UUID.fromString(userIdStr);
        boolean ok = deviceService.unbindDevice(deviceId, userId);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "BLE_DEVICE_NOT_FOUND", "message", "设备不存在",
                            "data", null, "traceId", traceId()));
        }
        return ResponseEntity.ok(Map.of("code", "OK", "message", "success", "data", null, "traceId", traceId()));
    }

    private static DeviceResponse toResponse(DeviceResult d) {
        return new DeviceResponse(d.id(), d.deviceType(), d.deviceName(),
                d.pushToken(), d.pushProvider(), d.lastOnlineAt(), d.createdAt());
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }
}
