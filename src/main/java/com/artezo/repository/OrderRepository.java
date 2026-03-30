package com.artezo.repository;

import com.artezo.dto.stats.orders.OrderStats;
import com.artezo.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<OrderEntity>findByOrderId(String orderId);

    @Query("""
        SELECT COUNT(o) as totalOrdersCount, 
               COALESCE(SUM(o.finalAmount), 0) as totalSpent 
        FROM OrderEntity o 
        WHERE o.user.userId = :userId
        """)
    OrderStats getOrderStatsByUserId(@Param("userId") Long userId);
}