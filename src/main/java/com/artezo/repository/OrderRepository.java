package com.artezo.repository;

import com.artezo.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    // Single order lookup by orderStrId
    Optional<OrderEntity> findByOrderStrId(String orderStrId);

    // User facing — my orders page (newest first)
    Page<OrderEntity> findByUserUserIdOrderByOrderDateDesc(Long userId, Pageable pageable);

    // Admin — all orders of ONE specific user (newest first)
    // Same query as above — reused via getOrdersByUserId()
    // findByUserUserIdOrderByOrderDateDesc() handles both cases ✅

    // Admin — ALL orders across ALL users (newest first)
    Page<OrderEntity> findAllByOrderByOrderDateDesc(Pageable pageable);

    // Check if SR order id already linked — prevents duplicate SR sync
    boolean existsByShiprocketOrderId(Long shiprocketOrderId);

    // Webhook lookup by AWB number
    Optional<OrderEntity> findByAwbNumber(String awbNumber);
}