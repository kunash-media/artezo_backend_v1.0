package com.artezo.schedulars.shiprokcet;


import com.artezo.entity.SRPreCheckoutEntity;

import com.artezo.enum_status.SRCheckoutStatus;
import com.artezo.repository.SRPreCheckoutRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs every 15 minutes.
 * Marks SRPreCheckoutEntity records that are still PENDING after 30 minutes
 * as ABANDONED — these are users who opened SR checkout but never completed payment.
 *
 * Requires @EnableScheduling on your Spring Boot main class or a @Configuration class.
 */
@Slf4j
@Component
public class SRAbandonedCheckoutJob {

    private final SRPreCheckoutRepository srPreCheckoutRepository;

    public SRAbandonedCheckoutJob(SRPreCheckoutRepository srPreCheckoutRepository) {
        this.srPreCheckoutRepository = srPreCheckoutRepository;
    }

    /** Runs every 15 minutes */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void markAbandonedCheckouts() {

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        List<SRPreCheckoutEntity> stale = srPreCheckoutRepository
                .findByStatusAndCreatedAtBefore(SRCheckoutStatus.PENDING, cutoff);

        if (stale.isEmpty()) return;

        stale.forEach(pre -> pre.setStatus(SRCheckoutStatus.ABANDONED));
        srPreCheckoutRepository.saveAll(stale);

        log.info("Marked {} checkout(s) as ABANDONED (older than 30 min)", stale.size());
    }
}