package com.artezo.util;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderCheckoutRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderRef(String orderRef);
    Optional<Order> findBySrOrderId(String srOrderId);
}