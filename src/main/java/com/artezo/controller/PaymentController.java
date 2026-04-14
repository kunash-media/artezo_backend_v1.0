package com.artezo.controller;

import com.artezo.dto.request.PaymentRequest;
import com.artezo.dto.request.PaymentVerificationRequest;
import com.artezo.dto.response.PaymentOrderDTO;
import com.artezo.dto.response.PaymentResponse;
import com.artezo.dto.response.PaymentVerificationResponse;
import com.artezo.entity.PaymentOrder;
import com.artezo.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private PaymentService paymentService;
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create a new payment order for a specific user
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createPaymentOrder(@RequestBody PaymentRequest request) {
        try {
            log.info("Creating payment order for userId: {} with amount: {}",
                    request.getUserId(), request.getAmount());
            PaymentResponse response = paymentService.createPaymentOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating payment order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating payment order: " + e.getMessage());
        }
    }

    /**
     * Verify payment after successful payment
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        try {
            log.info("Verifying payment for order: {}", request.getRazorpayOrderId());
            PaymentVerificationResponse response = paymentService.verifyPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error verifying payment: " + e.getMessage());
        }
    }

    /**
     * Get payment order by Razorpay order ID (existing functionality)
     */
    @GetMapping("/order/{razorpayOrderId}")
    public ResponseEntity<?> getPaymentOrder(@PathVariable String razorpayOrderId) {
        try {
            PaymentOrder paymentOrder = paymentService.getPaymentOrderByRazorpayId(razorpayOrderId);
            return ResponseEntity.ok(paymentOrder);
        } catch (Exception e) {
            log.error("Error fetching payment order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Payment order not found: " + e.getMessage());
        }
    }

    /**
     * Get payment order by Razorpay order ID for a specific user
     */
    @GetMapping("/user/{userId}/order/{razorpayOrderId}")
    public ResponseEntity<?> getPaymentOrderByUser(
            @PathVariable Long userId,
            @PathVariable String razorpayOrderId) {
        try {
            log.info("Fetching payment order: {} for userId: {}", razorpayOrderId, userId);
            PaymentOrder paymentOrder = paymentService.getPaymentOrderByRazorpayIdAndUserId(razorpayOrderId, userId);
            return ResponseEntity.ok(paymentOrder);
        } catch (Exception e) {
            log.error("Error fetching payment order for user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Payment order not found: " + e.getMessage());
        }
    }

    /**
     * Get all payment orders for a specific user
     */
    @GetMapping("/user/{userId}/orders")
    public ResponseEntity<?> getPaymentOrdersByUser(@PathVariable Long userId) {
        try {
            log.info("Fetching all payment orders for userId: {}", userId);
            List<PaymentOrder> paymentOrders = paymentService.getPaymentOrdersByUserId(userId);
            return ResponseEntity.ok(paymentOrders);
        } catch (Exception e) {
            log.error("Error fetching payment orders for user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error fetching payment orders: " + e.getMessage());
        }
    }

    /**
     * Get all payment orders for a specific user by status
     */
    @GetMapping("/user/{userId}/orders/status/{status}")
    public ResponseEntity<?> getPaymentOrdersByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable String status) {
        try {
            log.info("Fetching payment orders for userId: {} with status: {}", userId, status);
            List<PaymentOrder> paymentOrders = paymentService.getPaymentOrdersByUserIdAndStatus(userId, status);
            return ResponseEntity.ok(paymentOrders);
        } catch (Exception e) {
            log.error("Error fetching payment orders for user by status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error fetching payment orders: " + e.getMessage());
        }
    }

    /**
     * Get all payment orders (admin functionality)
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getAllPaymentOrders() {
        try {
            log.info("Fetching all payment orders");
            List<PaymentOrderDTO> paymentOrders = paymentService.getAllPaymentOrders();
            return ResponseEntity.ok(paymentOrders);
        } catch (Exception e) {
            log.error("Error fetching all payment orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching payment orders: " + e.getMessage());
        }
    }

    /**
     * Update payment status by Razorpay order ID
     */
    @PutMapping("/order/{razorpayOrderId}/status/{status}")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable String razorpayOrderId,
            @PathVariable String status) {
        try {
            log.info("Updating payment status for order: {} to status: {}", razorpayOrderId, status);
            PaymentOrder updatedOrder = paymentService.updatePaymentStatus(razorpayOrderId, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("Error updating payment status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating payment status: " + e.getMessage());
        }
    }
}