//package com.artezo.controller;
//
//import com.artezo.dto.request.ShiprocketOrderPayload;
//import com.artezo.dto.request.ShiprocketWebhookPayload;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//
//
//import java.util.Map;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/shiprocket")
//@RequiredArgsConstructor
//public class ShiprocketCheckoutController {
//
//    private final OrderService orderService;
//
//    /**
//     * CUSTOM ENDPOINT
//     * ──────────────
//     * Configure this URL in SR Checkout Dashboard → Settings → Custom Endpoints
//     * SR calls this after every order is placed. You must respond 200 OK within 5s.
//     *
//     * URL to put in SR dashboard: https://api.artezo.in/api/shiprocket/order-sync
//     */
//    @PostMapping("/order-sync")
//    public ResponseEntity<Map<String, String>> receiveOrder(
//            @RequestBody ShiprocketOrderPayload payload,
//            @RequestHeader(value = "X-Shiprocket-Hmac-Sha256", required = false) String hmacHeader) {
//
//        log.info("SR Order received: srOrderId={}, ref={}, status={}",
//                payload.srOrderId(), payload.orderName(), payload.financialStatus());
//
//        // Optional: verify HMAC signature
//        // boolean valid = hmacService.verify(payload, hmacHeader);
//        // if (!valid) return ResponseEntity.status(401).build();
//
//        try {
//            orderService.processShiprocketOrder(payload);
//            return ResponseEntity.ok(Map.of("status", "received", "order_id", payload.srOrderId()));
//        } catch (Exception e) {
//            log.error("Failed to process SR order {}: {}", payload.srOrderId(), e.getMessage(), e);
//            // Still return 200 — SR will retry on non-200 and spam your endpoint
//            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
//        }
//    }
//
//    /**
//     * WEBHOOK ENDPOINT
//     * ────────────────
//     * Configure in SR Checkout Dashboard → Settings → Webhooks → Add Webhook
//     * Type: "Real Time" for order events, "Abandon Cart" for cart recovery
//     *
//     * URL: https://api.artezo.in/api/shiprocket/webhook
//     */
//    @PostMapping("/webhook")
//    public ResponseEntity<Map<String, String>> receiveWebhook(
//            @RequestBody ShiprocketWebhookPayload payload) {
//
//        log.info("SR Webhook event={}, orderId={}, status={}",
//                payload.event(), payload.orderId(), payload.status());
//
//        try {
//            orderService.handleWebhookEvent(payload);
//        } catch (Exception e) {
//            log.error("Webhook processing error: {}", e.getMessage(), e);
//        }
//
//        // Always 200 — SR won't retry if you return errors
//        return ResponseEntity.ok(Map.of("received", "true"));
//    }
//
//    /**
//     * PRE-CHECKOUT (called by YOUR frontend before opening SR window)
//     * Creates a PENDING order in your DB so you have a record even if
//     * the user abandons mid-checkout.
//     */
//    @PostMapping("/pre-checkout")
//    public ResponseEntity<Map<String, Object>> preCheckout(@RequestBody Map<String, Object> body) {
//        String orderRef = (String) body.get("orderRef");
//        log.info("Pre-checkout initiated for ref={}", orderRef);
//
//        // Save pending order to your DB
//        orderService.createPendingOrder(orderRef, body);
//
//        return ResponseEntity.ok(Map.of(
//                "status", "pending",
//                "orderRef", orderRef
//        ));
//    }
//}
