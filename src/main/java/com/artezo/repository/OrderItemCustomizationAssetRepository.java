// ── OrderItemCustomizationAssetRepository.java ────────────────────────────
package com.artezo.repository;

import com.artezo.entity.OrderItemCustomizationAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemCustomizationAssetRepository
        extends JpaRepository<OrderItemCustomizationAssetEntity, Long> {

    List<OrderItemCustomizationAssetEntity> findByOrderItem_OrderItemIdOrderBySlotNumberAsc(
            Long orderItemId);
}