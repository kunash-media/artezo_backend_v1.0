package com.artezo.controller;

import com.artezo.dto.request.PaymentRequest;
import com.artezo.dto.request.PaymentVerificationRequest;
import com.artezo.dto.response.PaymentOrderDTO;
import com.artezo.dto.response.PaymentResponse;
import com.artezo.dto.response.PaymentVerificationResponse;
import com.artezo.entity.PaymentOrder;
import com.artezo.service.PaymentService;
import com.artezo.util.MagicCheckoutAddressCache;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final MagicCheckoutAddressCache magicAddressCache;
    private PaymentService paymentService;
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);


    @Value("${razorpay.webhook_secret:}")
    private String webhookSecret;
    

    public PaymentController(MagicCheckoutAddressCache magicAddressCache,
                             PaymentService paymentService) {
        this.magicAddressCache = magicAddressCache;
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


    // ADD this endpoint:
    @PostMapping("/magic-webhook")
    public ResponseEntity<String> handleMagicWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature",required = false) String signature) {

        // 1. Verify webhook signature production
//        try {
//            Mac mac = Mac.getInstance("HmacSHA256");
//            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
//            byte[] hash = mac.doFinal(payload.getBytes());
//            StringBuilder hex = new StringBuilder();
//            for (byte b : hash) {
//                String h = Integer.toHexString(0xff & b);
//                if (h.length() == 1) hex.append('0');
//                hex.append(h);
//            }
//            String computed = hex.toString();
//            if (!computed.equals(signature)) {
//                log.warn("Magic webhook signature mismatch");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
//            }
//        } catch (Exception e) {
//            log.error("Webhook signature error: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
//        }

        // for local test skipping X-Razorpay-Signature
        if (signature != null && !webhookSecret.isBlank()) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
                byte[] hash = mac.doFinal(payload.getBytes());
                StringBuilder hex = new StringBuilder();
                for (byte b : hash) {
                    String h = Integer.toHexString(0xff & b);
                    if (h.length() == 1) hex.append('0');
                    hex.append(h);
                }
                String computed = hex.toString();
                if (!computed.equals(signature)) {
                    log.warn("Magic webhook signature mismatch");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
                }
            } catch (Exception e) {
                log.error("Webhook signature error: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
            }
        } else {
            log.warn("Webhook signature check skipped — dev/test mode");
        }

        // 2. Parse and cache address
        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.optString("event");

            if ("magic_checkout.order.placed".equals(eventType)) {
                JSONObject entity = event
                        .getJSONObject("payload")
                        .getJSONObject("order")
                        .getJSONObject("entity");

                String razorpayOrderId = entity.optString("id");

                JSONObject customer = entity.optJSONObject("customer_details");
                JSONObject shipping = customer != null
                        ? customer.optJSONObject("shipping_address") : null;

                MagicCheckoutAddressCache.MagicAddressData data =
                        new MagicCheckoutAddressCache.MagicAddressData();

                if (customer != null) {
                    data.name  = customer.optString("name",  "");
                    data.email = customer.optString("email", "");
                    data.phone = customer.optString("contact", "")
                            .replace("+91", ""); // strip country code
                }
                if (shipping != null) {
                    data.address1 = shipping.optString("line1",       "");
                    data.address2 = shipping.optString("line2",       "");
                    data.city     = shipping.optString("city",        "");
                    data.state    = shipping.optString("state",       "");
                    data.pincode  = shipping.optString("zipcode",     "");
                }

                // Payment method from order
                data.paymentMethod = entity.optString("method", "PREPAID")
                        .equalsIgnoreCase("cod") ? "COD" : "PREPAID";

                magicAddressCache.store(razorpayOrderId, data);
                log.info("Magic address cached for order: {}", razorpayOrderId);
            }

        } catch (Exception e) {
            log.error("Magic webhook parse error: {}", e.getMessage());
        }

        // Always return 200 to Razorpay — they retry on non-200
        return ResponseEntity.ok("OK");
    }

}