package com.artezo.repository;

import com.artezo.entity.SRPreCheckoutEntity;
import com.artezo.enum_status.SRCheckoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SRPreCheckoutRepository extends JpaRepository<SRPreCheckoutEntity, Long> {

    Optional<SRPreCheckoutEntity> findByOrderRef(String orderRef);

    Optional<SRPreCheckoutEntity> findBySrOrderId(String srOrderId);

    boolean existsByOrderRef(String orderRef);

    /**
     * Used by scheduled job to mark stale PENDING records as ABANDONED.
     * e.g. find all PENDING older than 30 minutes.
     */
    List<SRPreCheckoutEntity> findByStatusAndCreatedAtBefore(
            SRCheckoutStatus status,
            LocalDateTime before
    );
}