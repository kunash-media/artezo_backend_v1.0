package com.artezo.schedulars.notification_alert;

import com.artezo.repository.AdminNotificationStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class NotificationCleanupScheduler {

    private final AdminNotificationStateRepository stateRepository;

    public NotificationCleanupScheduler(AdminNotificationStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    // Runs every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupStaleNotificationState() {
        // Delete entries older than 7 days — source alerts are already
        // outside the RECENT_DAYS window so these rows are orphaned anyway
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = stateRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Notification state cleanup → deleted {} stale entries", deleted);
    }
}
