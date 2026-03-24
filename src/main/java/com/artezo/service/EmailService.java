package com.artezo.service;

import java.math.BigDecimal;
import java.util.List;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp, String message);

    void sendOrderConfirmationEmail(String toEmail, String customerName,
                                           String orderId,
                                           BigDecimal totalAmount,
                                           List<String> productNames,
                                           String mobile);
}
