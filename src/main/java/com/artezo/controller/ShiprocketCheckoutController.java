package com.artezo.controller;

import com.artezo.service.ShiprocketAuthService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.util.*;

@RestController
@RequestMapping("/api/checkout")
public class ShiprocketCheckoutController {

    private static final Logger log = LoggerFactory.getLogger(ShiprocketCheckoutController.class);
    private final RestClient restClient;
    private final ShiprocketAuthService authService;

    @Value("${shiprocket.base-url}")
    private String shiprocketBaseUrl;

    // Your verified Store ID from Shiprocket dashboard
    private final String channelId = "8680913";

    public ShiprocketCheckoutController(RestClient restClient, ShiprocketAuthService authService) {
        this.restClient = restClient;
        this.authService = authService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateHeadlessCheckout(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody HeadlessCheckoutRequest request) {

        log.info("🚀 Initiating Shiprocket Headless Checkout Session for User: {}", userId);

        // 1. BUILD THE CORRECT PAYLOAD FOR /orders/create/adhoc
        // This is the format Shiprocket's adhoc endpoint expects
        Map<String, Object> cartPayload = buildAdhocOrderPayload(request, userId);

        try {
            // 2. FETCH VALID TOKEN
            String bearerToken = authService.getBearerToken();

            // 3. ✅ CORRECT TARGET URL
            // Using the base API URL + the actual orders creation endpoint
            // DO NOT use https://shiprocket.in (that's the marketing website)
            // DO NOT use https://checkout.shiprocket.in (that's the frontend SDK domain)
            String targetOrderCreateUrl = shiprocketBaseUrl + "/orders/create/adhoc";

            log.info("Sending payload to Shiprocket Orders API: {}", targetOrderCreateUrl);
            log.info("Payload: {}", cartPayload);

            // 4. CALL THE API
            Map<?, ?> response = restClient.post()
                    .uri(targetOrderCreateUrl)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(cartPayload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("checkout_url")) {
                String checkoutUrl = (String) response.get("checkout_url");
                log.info("✅ Shiprocket Checkout URL generated successfully");
                log.info("Checkout URL: {}", checkoutUrl);

                return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
            }

            log.error("❌ Checkout initialization failed — response missing checkout_url");
            log.error("Response body: {}", response);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Shiprocket did not return checkout_url", "details", response));

        } catch (org.springframework.web.client.RestClientResponseException ex) {
            // CAPTURES EXACT ERROR FROM SHIPROCKET
            String errorResponseBody = ex.getResponseBodyAsString();
            log.error("❌ Shiprocket API Error Status: {}", ex.getStatusCode());
            log.error("Error Body: {}", errorResponseBody);
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", "Shiprocket API error", "status", ex.getStatusCode().toString(), "details", errorResponseBody));

        } catch (Exception e) {
            log.error("❌ Exception during headless initialization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initialize checkout", "message", e.getMessage()));
        }
    }

    /**
     * Builds the payload in the format that Shiprocket's /orders/create/adhoc endpoint expects.
     * This is different from a cart checkout — it's a complete order creation.
     */
    private Map<String, Object> buildAdhocOrderPayload(HeadlessCheckoutRequest request, Long userId) {
        Map<String, Object> payload = new HashMap<>();

        // Channel/Store ID
        payload.put("channel_id", Integer.parseInt(channelId));

        // Customer reference (use user ID)
        Map<String, String> customerDetails = new HashMap<>();
        customerDetails.put("customer_id", String.valueOf(userId));
        customerDetails.put("customer_email", ""); // Will be filled by checkout page
        customerDetails.put("customer_phone", ""); // Will be filled by checkout page
        customerDetails.put("customer_name", "");  // Will be filled by checkout page
        payload.put("customer_details", customerDetails);

        // Order items — correctly formatted for adhoc endpoint
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();

        item.put("name", request.getProductName());
        item.put("sku", request.getSku());
        item.put("units", request.getQuantity());
        item.put("selling_price", request.getUnitPrice());

        // Include variant_id only if provided, otherwise use product ID
        if (request.getVariantId() != null && !request.getVariantId().isEmpty()) {
            item.put("variant_id", request.getVariantId());
        }

        items.add(item);
        payload.put("order_items", items);

        // Totals — PRE-TAX amount (backend will calculate tax + shipping)
        double itemTotal = request.getUnitPrice() * request.getQuantity();
        payload.put("order_amount", itemTotal);  // Subtotal before tax/shipping
        payload.put("total_amount", itemTotal);  // Shiprocket will add taxes

        // Optional: order reference (your internal order ID prefix)
        payload.put("order_id", "BN-" + System.currentTimeMillis()); // Temporary ID for checkout

        log.info("✅ Adhoc order payload built:");
        log.info("  › Channel: {}", channelId);
        log.info("  › Product: {}", request.getProductName());
        log.info("  › Quantity: {}", request.getQuantity());
        log.info("  › Unit Price: ₹{}", request.getUnitPrice());
        log.info("  › Item Total: ₹{}", itemTotal);

        return payload;
    }

    @Data
    public static class HeadlessCheckoutRequest {
        private String productStrId;
        private String productName;
        private String variantId;
        private String sku;
        private Integer quantity;
        private Double unitPrice;
    }
}