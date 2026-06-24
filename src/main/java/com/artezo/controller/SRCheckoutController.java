package com.artezo.controller;

import com.artezo.entity.ProductEntity;
import com.artezo.entity.ProductVariantEntity;
import com.artezo.entity.SRPreCheckoutEntity;
import com.artezo.enum_status.SRCheckoutStatus;
import com.artezo.repository.ProductRepository;
import com.artezo.repository.SRPreCheckoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * SR Checkout Integration Controller
 *
 * 1. POST /api/shiprocket/access-token  — generate SR checkout access token (called by YOUR frontend)
 * 2. POST /api/shiprocket/order-details — get order details from SR (called by YOUR frontend on return)
 * 3. POST /api/shiprocket/catalog/product    — push single product to SR (catalog webhook TO SR)
 * 4. POST /api/shiprocket/catalog/collection — push single collection to SR (catalog webhook TO SR)
 */
@Slf4j
@RestController
@RequestMapping("/api/shiprocket")
@RequiredArgsConstructor
public class SRCheckoutController {

    @Value("${shiprocket.checkout.api-key}")
    private String apiKey;

    @Value("${shiprocket.checkout.api-secret}")
    private String apiSecret;

    // PROD: https://checkout-api.shiprocket.com
    // STAGING: https://fastrr-api-dev.pickrr.com
    @Value("${shiprocket.checkout.base-url:https://checkout-api.shiprocket.com}")
    private String srBaseUrl;

    private final ProductRepository       productRepository;
    private final SRPreCheckoutRepository srPreCheckoutRepository;
    private final RestTemplate            restTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    //  1. GENERATE ACCESS TOKEN
    //     YOUR frontend calls this on Buy Now click.
    //     This calls SR API, gets a token, frontend passes token to HeadlessCheckout.addToCart()
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/access-token")
    public ResponseEntity<Map<String, Object>> generateAccessToken(
            @RequestBody SRAccessTokenRequest req) {

        String orderRef  = req.orderRef();
        String variantId = req.variantId();
        int    quantity  = req.quantity();

        // ── Save pre-checkout PENDING record ──
        if (!srPreCheckoutRepository.existsByOrderRef(orderRef)) {
            SRPreCheckoutEntity pre = new SRPreCheckoutEntity();
            pre.setOrderRef(orderRef);
            pre.setStatus(SRCheckoutStatus.PENDING);
            pre.setProductStrId(req.productStrId());
            pre.setProductName(req.productName());
            pre.setVariantId(req.variantId());
            pre.setVariantLabel(req.variantLabel());
            pre.setSku(req.sku());
            pre.setQuantity(quantity);
            pre.setUnitPrice(req.unitPrice());
            pre.setMrp(req.mrp());
            pre.setTotalAmount(req.unitPrice() * quantity);
            pre.setSource("SR_HOT_CHECKOUT");
            srPreCheckoutRepository.save(pre);
        }

        // ── Build SR access-token request body ──
        Map<String, Object> cartItem = new LinkedHashMap<>();
        cartItem.put("variant_id", variantId);
        cartItem.put("quantity",   quantity);

        // If custom price needed (your selling price != catalog price)
        if (req.unitPrice() != null) {
            Map<String, Object> catalogData = new LinkedHashMap<>();
            catalogData.put("price",     req.unitPrice());
            catalogData.put("name",      req.productName());
            catalogData.put("image_url", req.imageUrl() != null ? req.imageUrl() : "");
            cartItem.put("catalog_data", catalogData);
        }

        Map<String, Object> cartData = new LinkedHashMap<>();
        cartData.put("items",             List.of(cartItem));
        cartData.put("mobile_app",        false);
        cartData.put("custom_attributes", Map.of("order_ref", orderRef));

        // Coupon if applicable
        if (req.couponCode() != null && req.couponDiscount() != null) {
            cartData.put("cart_discount", Map.of(
                    "coupon_code", req.couponCode(),
                    "amount",      req.couponDiscount()
            ));
        }

        Map<String, Object> srPayload = new LinkedHashMap<>();
        srPayload.put("cart_data",    cartData);
        srPayload.put("redirect_url", req.redirectUrl());
        srPayload.put("timestamp",    Instant.now().toString());

        // ── Call SR API ──
        try {
            String    body    = toJson(srPayload);
            String    hmac    = computeHmac(body, apiSecret);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key",        apiKey);
            headers.set("X-Api-HMAC-SHA256", hmac);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> srResp = restTemplate.postForEntity(
                    srBaseUrl + "/api/v1/access-token/checkout",
                    entity, Map.class);

            log.info("SR access-token generated for orderRef={}", orderRef);
            return ResponseEntity.ok(Objects.requireNonNull(srResp.getBody()));

        } catch (Exception e) {
            log.error("SR access-token generation failed for orderRef={}: {}", orderRef, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate checkout token: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. GET ORDER DETAILS FROM SR
    //     Call after user returns from SR checkout (on redirect_url page)
    //     Pass the oid param SR appends to your redirect_url
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/order-details")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@RequestBody Map<String, String> body) {

        String oid = body.get("oid"); // SR appends ?oid=xxx to redirect_url
        if (oid == null || oid.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "oid is required"));
        }

        try {
            String timestamp = Instant.now().toString();
            Map<String, Object> reqBody = Map.of(
                    "order_id",  oid,
                    "timestamp", timestamp
            );

            String      reqJson = toJson(reqBody);
            String      hmac    = computeHmac(reqJson, apiSecret);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key",         apiKey);
            headers.set("X-Api-HMAC-SHA256",  hmac);

            HttpEntity<String> entity = new HttpEntity<>(reqJson, headers);
            ResponseEntity<Map> srResp = restTemplate.postForEntity(
                    srBaseUrl + "/api/v1/custom-platform-order/details",
                    entity, Map.class);

            return ResponseEntity.ok(Objects.requireNonNull(srResp.getBody()));

        } catch (Exception e) {
            log.error("SR order-details fetch failed for oid={}: {}", oid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. PUSH PRODUCT TO SR (Catalog Webhook — you call when product changes)
    //     Call this from your product create/update service
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/catalog/product/push")
    public ResponseEntity<Map<String, Object>> pushProductToSR(@RequestParam String productStrId) {

        ProductEntity p = productRepository.findByProductStrId(productStrId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productStrId));

        Map<String, Object> payload = buildProductPayload(p);

        return callSRCatalogWebhook(
                srBaseUrl + "/wh/v1/custom/product",
                payload,
                "product push: " + productStrId
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  4. PUSH COLLECTION TO SR (Catalog Webhook — you call when category changes)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/catalog/collection/push")
    public ResponseEntity<Map<String, Object>> pushCollectionToSR(
            @RequestBody Map<String, Object> collectionPayload) {

        return callSRCatalogWebhook(
                srBaseUrl + "/wh/v1/custom/collection",
                collectionPayload,
                "collection push"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Call SR catalog webhook endpoint
    // ─────────────────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> callSRCatalogWebhook(
            String url, Map<String, Object> payload, String logCtx) {
        try {
            String      json    = toJson(payload);
            String      hmac    = computeHmac(json, apiSecret);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key",         apiKey);
            headers.set("X-Api-HMAC-SHA256",  hmac);

            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<Map> resp  = restTemplate.postForEntity(url, entity, Map.class);
            log.info("SR catalog webhook OK [{}] status={}", logCtx, resp.getStatusCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> body = resp.getBody() != null ? resp.getBody() : Map.of("status", "ok");
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("SR catalog webhook FAILED [{}]: {}", logCtx, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Build SR-shaped product payload from ProductEntity
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> buildProductPayload(ProductEntity p) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id",           p.getProductPrimeId());
        product.put("title",        p.getProductName());
        product.put("body_html",    p.getDescription() != null ? p.getDescription() : "");
        product.put("vendor",       p.getBrandName() != null ? p.getBrandName() : "Artezo");
        product.put("product_type", p.getProductCategory() != null ? p.getProductCategory() : "");
        product.put("handle",       toHandle(p.getProductName()));
        product.put("tags",         p.getGlobalTags() != null ? p.getGlobalTags() : "");
        product.put("status",       p.getIsDeleted() ? "draft" : "active");
        product.put("created_at",   formatDate(p.getCreatedAt()));
        product.put("updated_at",   formatDate(p.getUpdatedAt()));
        product.put("image",        Map.of("src", buildImageUrl(p.getProductStrId(), null)));

        List<Map<String, Object>> variants = new ArrayList<>();
        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            for (ProductVariantEntity v : p.getVariants()) {
                Map<String, Object> variant = new LinkedHashMap<>();
                variant.put("id",               v.getId());
                variant.put("title",            v.getTitleName() != null ? v.getTitleName() : "Default");
                variant.put("sku",              v.getSku());
                variant.put("price",            String.format("%.2f", v.getPrice()));
                variant.put("compare_at_price", String.format("%.2f", v.getMrp() != null ? v.getMrp() : v.getPrice()));
                variant.put("quantity",         v.getStock() != null ? v.getStock() : 0);
                variant.put("taxable",          true);
                variant.put("grams",            toGrams(v.getWeight()));
                variant.put("weight",           v.getWeight() != null ? v.getWeight() : 0.5);
                variant.put("weight_unit",      "kg");
                variant.put("created_at",       formatDate(p.getCreatedAt()));
                variant.put("updated_at",       formatDate(p.getUpdatedAt()));
                variant.put("image",            Map.of("src", buildImageUrl(p.getProductStrId(), v.getVariantId())));
                Map<String, Object> opts = new LinkedHashMap<>();
                if (v.getColor() != null) opts.put("Color", v.getColor());
                if (v.getSize()  != null) opts.put("Size",  v.getSize());
                variant.put("option_values", opts);
                variants.add(variant);
            }
        } else {
            // Default single variant
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("id",               p.getProductPrimeId());
            def.put("title",            "Default");
            def.put("sku",              p.getCurrentSku());
            def.put("price",            String.format("%.2f", p.getCurrentSellingPrice() != null ? p.getCurrentSellingPrice() : 0.0));
            def.put("compare_at_price", String.format("%.2f", p.getCurrentMrpPrice()    != null ? p.getCurrentMrpPrice()    : 0.0));
            def.put("quantity",         p.getCurrentStock() != null ? p.getCurrentStock() : 0);
            def.put("taxable",          true);
            def.put("grams",            toGrams(p.getWeight()));
            def.put("weight",           p.getWeight() != null ? p.getWeight() : 0.5);
            def.put("weight_unit",      "kg");
            def.put("created_at",       formatDate(p.getCreatedAt()));
            def.put("updated_at",       formatDate(p.getUpdatedAt()));
            def.put("image",            Map.of("src", buildImageUrl(p.getProductStrId(), null)));
            def.put("option_values",    new LinkedHashMap<>());
            variants.add(def);
        }
        product.put("variants", variants);

        // Options
        Set<String> colors = new LinkedHashSet<>();
        Set<String> sizes  = new LinkedHashSet<>();
        if (p.getVariants() != null) {
            for (ProductVariantEntity v : p.getVariants()) {
                if (v.getColor() != null) colors.add(v.getColor());
                if (v.getSize()  != null) sizes.add(v.getSize());
            }
        }
        List<Map<String, Object>> options = new ArrayList<>();
        if (!colors.isEmpty()) options.add(Map.of("name", "Color", "values", new ArrayList<>(colors)));
        if (!sizes.isEmpty())  options.add(Map.of("name", "Size",  "values", new ArrayList<>(sizes)));
        product.put("options", options);

        return product;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE UTILS
    // ─────────────────────────────────────────────────────────────────────────

    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    private String buildImageUrl(String productStrId, String variantId) {
        String url = "https://api.artezo.in/api/v1/products/" + productStrId + "/image";
        if (variantId != null) url += "?variantId=" + variantId;
        return url;
    }

    private String toHandle(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private int toGrams(Double kg) {
        return kg != null ? (int)(kg * 1000) : 500;
    }

    private String formatDate(java.time.LocalDateTime dt) {
        return dt != null ? dt.toString() + "+05:30" : "2024-01-01T00:00:00+05:30";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REQUEST RECORD — for access-token endpoint
    // ─────────────────────────────────────────────────────────────────────────
    public record SRAccessTokenRequest(
            String  orderRef,
            String  productStrId,
            String  productName,
            String  variantId,      // SR variant_id (your ProductVariantEntity.id as String)
            String  variantLabel,
            String  sku,
            Integer quantity,
            Double  unitPrice,
            Double  mrp,
            String  imageUrl,
            String  redirectUrl,    // your order-success page URL
            String  couponCode,     // optional
            Double  couponDiscount  // optional
    ) {}
}