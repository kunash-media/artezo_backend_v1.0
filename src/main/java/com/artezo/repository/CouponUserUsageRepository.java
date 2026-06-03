package com.artezo.repository;

import com.artezo.entity.CouponUserUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponUserUsageRepository extends JpaRepository<CouponUserUsage, Long> {
    Optional<CouponUserUsage> findByCouponIdAndUserId(Long couponId, Long userId);
}