package com.artezo.schedulars.coupons_validity;


import com.artezo.entity.CouponEntity;
import com.artezo.repository.CouponRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Component
public class CouponResetScheduler {

    private static final Logger LOGGER = Logger.getLogger(CouponResetScheduler.class.getName());
    private final CouponRepository couponRepository;

    public CouponResetScheduler(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }


    @Scheduled(fixedDelay = 86400000)
    public void resetExpiredCoupons() {
        LOGGER.info("Cron job running: resetting expired coupons");
        LocalDateTime now = LocalDateTime.now();

        List<CouponEntity> expired = couponRepository.findExpiredCoupons(now);
        expired.forEach(c -> {
            c.setCouponUsed(false);
            c.setIsActive(false);
        });

        couponRepository.saveAll(expired);
        LOGGER.info("Cron job done: reset " + expired.size() + " coupons");
    }
}
