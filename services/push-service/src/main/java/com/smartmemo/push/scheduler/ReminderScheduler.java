package com.smartmemo.push.scheduler;

import com.smartmemo.push.application.PushApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.*;

/**
 * 定时检查到期的提醒，自动创建推送通知。
 * 每分钟运行一次。
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final DataSource dataSource;
    private final PushApplicationService pushService;
    private final RestTemplate restTemplate;
    private final String syncServiceUrl;

    public ReminderScheduler(DataSource dataSource,
                              PushApplicationService pushService,
                              @Value("${sync.service.url:http://localhost:8083}") String syncServiceUrl) {
        this.dataSource = dataSource;
        this.pushService = pushService;
        this.restTemplate = new RestTemplate();
        this.syncServiceUrl = syncServiceUrl;
    }

    @Scheduled(fixedRate = 60_000) // 每分钟
    public void checkReminders() {
        try (Connection conn = dataSource.getConnection()) {
            // 查找 remind_at 已到期、未被删除、未完成、且尚未推送过的备忘录
            String sql = """
                SELECT m.id, m.user_id, m.title, m.remind_at
                FROM memos m
                WHERE m.remind_at IS NOT NULL
                  AND m.remind_at <= ?
                  AND m.status = 'active'
                  AND m.deleted_at IS NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM push_messages p
                    WHERE p.memo_id = m.id AND p.type = 'reminder'
                  )
                LIMIT 50
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                UUID memoId = UUID.fromString(rs.getString("id"));
                UUID userId = UUID.fromString(rs.getString("user_id"));
                String title = rs.getString("title");

                pushService.create(userId, memoId, "reminder",
                        "⏰ 提醒", "备忘录「" + title + "」的提醒时间到了");

                // 通知 sync-service 推送 WebSocket
                notifySyncService(userId, memoId, "reminder_triggered");

                count++;
            }
            if (count > 0) {
                log.info("Reminder check: created {} push messages", count);
            }
        } catch (Exception e) {
            log.error("Reminder check failed", e);
        }
    }

    private void notifySyncService(UUID userId, UUID memoId, String type) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", userId.toString());

            Map<String, Object> body = Map.of(
                    "type", type,
                    "memoId", memoId.toString()
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(syncServiceUrl + "/internal/notify", request, String.class);
        } catch (Exception e) {
            log.warn("Failed to notify sync-service: {}", e.getMessage());
        }
    }
}
