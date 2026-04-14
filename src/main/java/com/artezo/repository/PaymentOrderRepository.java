package com.artezo.repository;

import com.artezo.entity.PaymentOrder;
import com.artezo.enum_status.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);
    Optional<PaymentOrder> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<PaymentOrder> findByReceipt(String receipt);

    // New methods for user-based queries
    List<PaymentOrder> findByUserUserId(Long userId);
    List<PaymentOrder> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PaymentOrder> findByRazorpayOrderIdAndUserUserId(String razorpayOrderId, Long userId);

    // Optional: For additional filtering
    List<PaymentOrder> findByUserUserIdAndStatus(Long userId, PaymentStatus status);

    @Query("SELECT po FROM PaymentOrder po")
    List<PaymentOrder> findAllPaymentOrders();
}