package com.smartmemo.user.application;

import com.smartmemo.user.domain.UserDevice;
import com.smartmemo.user.infrastructure.persistence.UserDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 设备管理应用服务。
 */
@Service
public class DeviceApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceApplicationService.class);

    private final UserDeviceRepository deviceRepository;

    public DeviceApplicationService(UserDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * 获取用户的所有设备。
     */
    public List<DeviceResult> listDevices(UUID userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(DeviceResult::from)
                .toList();
    }

    /**
     * 绑定新设备。
     */
    @Transactional
    public DeviceResult bindDevice(UUID userId, String deviceType, String deviceName,
                                    String pushToken, String pushProvider) {
        UserDevice device = new UserDevice();
        device.setUserId(userId);
        device.setDeviceType(deviceType);
        device.setDeviceName(deviceName);
        device.setPushToken(pushToken);
        device.setPushProvider(pushProvider);
        device.setLastOnlineAt(Instant.now());

        UserDevice saved = deviceRepository.save(device);
        log.info("Device bound: userId={}, deviceId={}, type={}", userId, saved.getId(), deviceType);
        return DeviceResult.from(saved);
    }

    /**
     * 解绑设备（软删除）。
     */
    @Transactional
    public boolean unbindDevice(UUID deviceId, UUID userId) {
        Optional<UserDevice> opt = deviceRepository.findById(deviceId);
        if (opt.isEmpty()) return false;

        UserDevice device = opt.get();
        if (!device.getUserId().equals(userId)) return false;

        device.setDeletedAt(Instant.now());
        deviceRepository.save(device);
        log.info("Device unbound: userId={}, deviceId={}", userId, deviceId);
        return true;
    }

    public record DeviceResult(UUID id, String deviceType, String deviceName,
                                String pushToken, String pushProvider,
                                Instant lastOnlineAt, Instant createdAt) {
        public static DeviceResult from(UserDevice device) {
            return new DeviceResult(device.getId(), device.getDeviceType(), device.getDeviceName(),
                    device.getPushToken(), device.getPushProvider(),
                    device.getLastOnlineAt(), device.getCreatedAt());
        }
    }
}
