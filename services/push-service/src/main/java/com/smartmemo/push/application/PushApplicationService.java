package com.smartmemo.push.application;

import com.smartmemo.push.domain.PushMessage;
import com.smartmemo.push.infrastructure.PushMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PushApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PushApplicationService.class);

    private final PushMessageRepository repository;

    public PushApplicationService(PushMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PushMessage create(UUID userId, UUID memoId, String type, String title, String body) {
        PushMessage msg = new PushMessage(userId, memoId, type, title, body);
        PushMessage saved = repository.save(msg);
        log.info("Push created: userId={}, type={}, title={}", userId, type, title);
        return saved;
    }

    public List<PushMessage> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<PushMessage> listUnread(UUID userId) {
        return repository.findUnreadByUserId(userId);
    }

    public long countUnread(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Optional<PushMessage> markRead(UUID messageId, UUID userId) {
        Optional<PushMessage> msg = repository.findById(messageId);
        msg.ifPresent(m -> {
            if (m.getUserId().equals(userId)) {
                m.setRead(true);
                repository.save(m);
            }
        });
        return msg;
    }

    @Transactional
    public void markAllRead(UUID userId) {
        List<PushMessage> unread = repository.findUnreadByUserId(userId);
        unread.forEach(m -> m.setRead(true));
        repository.saveAll(unread);
    }
}
