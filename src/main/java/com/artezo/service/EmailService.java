package com.artezo.service;

import com.artezo.dto.response.OrderResponse;

import java.math.BigDecimal;
import java.util.List;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp, String message);

    // EmailService.java (Interface)
    void sendOrderConfirmationEmail(
            String toEmail,
            String customerName,
            String orderId,
            BigDecimal totalAmount,
            List<OrderResponse.OrderItemResponse> orderItems,  // Changed name + type
            String mobile);
}
