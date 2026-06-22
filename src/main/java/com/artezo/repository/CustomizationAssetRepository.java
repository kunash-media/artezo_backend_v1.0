package com.artezo.repository;

import com.artezo.entity.CustomizationAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomizationAssetRepository extends JpaRepository<CustomizationAssetEntity, Long> {

    // Used in upload response lookup
    Optional<CustomizationAssetEntity> findByAssetUuid(String assetUuid);

    // GC scheduler: find PENDING assets past their expiry
    List<CustomizationAssetEntity> findByStatusAndExpiresAtBefore(
            CustomizationAssetEntity.AssetStatus status,
            LocalDateTime cutoff
    );

    // Bulk soft-expire before physical delete
    @Modifying
    @Query("UPDATE CustomizationAssetEntity a SET a.status = 'EXPIRED' " +
            "WHERE a.status = 'PENDING' AND a.expiresAt < :cutoff")
    int markExpiredPendingAssets(@Param("cutoff") LocalDateTime cutoff);
}