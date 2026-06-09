package com.smartmemo.user.application;

import com.smartmemo.user.domain.User;
import com.smartmemo.user.domain.UserStatus;
import com.smartmemo.user.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户应用服务。
 */
@Service
public class UserApplicationService {

    private final UserRepository userRepository;

    public UserApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 获取当前用户信息。
     */
    public Optional<UserResult> getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .map(UserResult::from);
    }

    /**
     * 更新用户信息（部分字段）。
     */
    @Transactional
    public Optional<UserResult> updateUser(UUID userId, String email, String phone) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        userRepository.save(user);
        return Optional.of(UserResult.from(user));
    }

    public record UserResult(UUID id, String username, String email, String phone,
                              UserStatus status, java.time.Instant createdAt) {
        public static UserResult from(User user) {
            return new UserResult(user.getId(), user.getUsername(), user.getEmail(),
                    user.getPhone(), user.getStatus(), user.getCreatedAt());
        }
    }
}
