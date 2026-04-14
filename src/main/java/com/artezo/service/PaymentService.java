package com.artezo.service;


import com.artezo.dto.request.PaymentRequest;
import com.artezo.dto.request.PaymentVerificationRequest;
import com.artezo.dto.response.PaymentOrderDTO;
import com.artezo.dto.response.PaymentResponse;
import com.artezo.dto.response.PaymentVerificationResponse;
import com.artezo.entity.PaymentOrder;

import java.util.List;

public interface PaymentService {

    /**
     * Create a new payment order
     */
    PaymentResponse createPaymentOrder(PaymentRequest request) throws Exception;

    /**
     * Verify payment signature
     */
    PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request) throws Exception;

    /**
     * Get payment order by Razorpay order ID
     */
    PaymentOrder getPaymentOrderByRazorpayId(String razorpayOrderId);

    /**
     * Get all payment orders
     */
    List<PaymentOrderDTO> getAllPaymentOrders();

    /**
     * Update payment status
     */
    PaymentOrder updatePaymentStatus(String razorpayOrderId, String status);

    // New methods for user-based operations
    List<PaymentOrder> getPaymentOrdersByUserId(Long userId);

    PaymentOrder getPaymentOrderByRazorpayIdAndUserId(String razorpayOrderId, Long userId);

    List<PaymentOrder> getPaymentOrdersByUserIdAndStatus(Long userId, String status);
}