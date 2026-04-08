package com.artezo.repository;

import com.artezo.entity.AdminNotificationStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminNotificationStateRepository extends JpaRepository<AdminNotificationStateEntity, Long> {

    List<AdminNotificationStateEntity> findByAdminId(String adminId);

    Optional<AdminNotificationStateEntity> findByAdminIdAndFingerprint(String adminId, String fingerprint);

    boolean existsByAdminIdAndFingerprint(String adminId, String fingerprint);

    @Modifying
    @Query("DELETE FROM AdminNotificationStateEntity n WHERE n.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}