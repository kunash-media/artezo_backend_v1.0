package com.artezo.service;

import com.artezo.dto.request.ShiprocketOrderRequest;
import com.artezo.dto.request.ShiprocketReturnRequest;
import com.artezo.dto.response.ShiprocketOrderResponse;
import com.artezo.entity.OrderEntity;
import com.artezo.entity.OrderItemEntity;
import com.artezo.enum_status.OrderStatus;
import com.artezo.enum_status.PaymentMethod;
import com.artezo.enum_status.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShiprocketService {

    private static final Logger log = LoggerFactory.getLogger(ShiprocketService.class);
    private static final DateTimeFormatter SR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient restClient;
    private final ShiprocketAuthService authService;

    @Value("${shiprocket.pickup-location}")
    private String pickupLocation;

    // ── Your warehouse details for return orders ──────────────────────────
    // Add these to application.properties:
    // shiprocket.warehouse.name=Your Store Name
    // shiprocket.warehouse.phone=9876543210
    // shiprocket.warehouse.address=Your Warehouse Address
    // shiprocket.warehouse.city=Mumbai
    // shiprocket.warehouse.state=Maharashtra
    // shiprocket.warehouse.pincode=400001

    @Value("${shiprocket.warehouse.name}")
    private String warehouseName;

    @Value("${shiprocket.warehouse.phone}")
    private String warehousePhone;

    @Value("${shiprocket.warehouse.address}")
    private String warehouseAddress;

    @Value("${shiprocket.warehouse.city}")
    private String warehouseCity;

    @Value("${shiprocket.warehouse.state}")
    private String warehouseState;

    @Value("${shiprocket.warehouse.pincode}")
    private String warehousePincode;

    public ShiprocketService(@Qualifier("shiprocketRestClient") RestClient restClient,
                             ShiprocketAuthService authService) {
        this.restClient  = restClient;
        this.authService = authService;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CREATE ORDER
    //  Called simultaneously when your /api/orders/create is hit (Cart flow)
    // ────────────────────────────────────────────────────────────────────────

    public ShiprocketOrderResponse createOrder(OrderEntity order) {
        log.info("Creating Shiprocket order for: {}", order.getOrderStrId());

        ShiprocketOrderRequest request = buildCreateOrderRequest(order);

        try {
            ShiprocketOrderResponse response = executeWithTokenRetry(() ->
                    restClient.post()
                            .uri("/orders/create/adhoc")
                            .header("Authorization", authService.getBearerToken())
                            .body(request)
                            .retrieve()
                            .body(ShiprocketOrderResponse.class)
            );

            log.info("Shiprocket order created — SR Order ID: {}, Shipment ID: {}",
                    response.getOrderId(), response.getShipmentId());
            return response;

        } catch (Exception e) {
            log.error("Failed to create Shiprocket order for {}: {}", order.getOrderStrId(), e.getMessage());
            throw new RuntimeException("Shiprocket createOrder failed: " + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CANCEL ORDER
    //  Called from your /api/orders/{orderId}/cancel endpoint
    // ────────────────────────────────────────────────────────────────────────

    public void cancelOrder(Long shiprocketOrderId) {
        log.info("Cancelling Shiprocket order ID: {}", shiprocketOrderId);

        Map<String, Object> body = Map.of("ids", List.of(shiprocketOrderId));

        try {
            executeWithTokenRetry(() ->
                    restClient.post()
                            .uri("/orders/cancel")
                            .header("Authorization", authService.getBearerToken())
                            .body(body)
                            .retrieve()
                            .body(Map.class)
            );
            log.info("Shiprocket order {} cancelled successfully", shiprocketOrderId);

        } catch (Exception e) {
            log.error("Failed to cancel Shiprocket order {}: {}", shiprocketOrderId, e.getMessage());
            throw new RuntimeException("Shiprocket cancelOrder failed: " + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CREATE RETURN ORDER
    //  Called from your /api/orders/{orderId}/return endpoint
    // ────────────────────────────────────────────────────────────────────────

    public ShiprocketOrderResponse createReturnOrder(OrderEntity order) {
        log.info("Creating Shiprocket return order for: {}", order.getOrderStrId());

        ShiprocketReturnRequest request = buildReturnOrderRequest(order);

        try {
            ShiprocketOrderResponse response = executeWithTokenRetry(() ->
                    restClient.post()
                            .uri("/orders/create/return")
                            .header("Authorization", authService.getBearerToken())
                            .body(request)
                            .retrieve()
                            .body(ShiprocketOrderResponse.class)
            );

            log.info("Shiprocket return order created — SR Order ID: {}", response.getOrderId());
            return response;

        } catch (Exception e) {
            log.error("Failed to create Shiprocket return for {}: {}", order.getOrderStrId(), e.getMessage());
            throw new RuntimeException("Shiprocket createReturnOrder failed: " + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  EXCHANGE ORDER
    //  Exchange = Step 1: Return old item + Step 2: New forward order
    //  Returns [returnResponse, newForwardResponse]
    // ────────────────────────────────────────────────────────────────────────

    public ShiprocketOrderResponse[] exchangeOrder(OrderEntity originalOrder,
                                                   OrderEntity replacementOrder) {
        log.info("Initiating Shiprocket exchange for original order: {}", originalOrder.getOrderStrId());

        // Step 1 — Return old item from customer
        ShiprocketOrderResponse returnResponse = createReturnOrder(originalOrder);

        // Step 2 — Send new replacement item to customer
        ShiprocketOrderResponse forwardResponse = createOrder(replacementOrder);

        log.info("Exchange complete — Return SR ID: {}, New Forward SR ID: {}",
                returnResponse.getOrderId(), forwardResponse.getOrderId());

        return new ShiprocketOrderResponse[]{ returnResponse, forwardResponse };
    }

    // ────────────────────────────────────────────────────────────────────────
    //  TRACK SHIPMENT BY AWB
    //  Called from your /api/orders/{orderId}/track endpoint
    // ────────────────────────────────────────────────────────────────────────

    public Map trackByAwb(String awbNumber) {
        log.info("Tracking shipment with AWB: {}", awbNumber);

        try {
            return executeWithTokenRetry(() ->
                    restClient.get()
                            .uri("/courier/track/awb/" + awbNumber)
                            .header("Authorization", authService.getBearerToken())
                            .retrieve()
                            .body(Map.class)
            );
        } catch (Exception e) {
            log.error("Failed to track AWB {}: {}", awbNumber, e.getMessage());
            throw new RuntimeException("Shiprocket tracking failed: " + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CHECK SERVICEABILITY
    //  Use before order creation to check if pincode is deliverable
    // ────────────────────────────────────────────────────────────────────────

    public Map checkServiceability(String pickupPincode, String deliveryPincode,
                                   Double weight, Boolean cod) {
        log.info("Checking serviceability: {} → {}", pickupPincode, deliveryPincode);

        try {
            return executeWithTokenRetry(() ->
                    restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/courier/serviceability/")
                                    .queryParam("pickup_postcode", pickupPincode)
                                    .queryParam("delivery_postcode", deliveryPincode)
                                    .queryParam("weight", weight)
                                    .queryParam("cod", cod ? 1 : 0)
                                    .build())
                            .header("Authorization", authService.getBearerToken())
                            .retrieve()
                            .body(Map.class)
            );
        } catch (Exception e) {
            log.error("Serviceability check failed: {}", e.getMessage());
            throw new RuntimeException("Shiprocket serviceability check failed: " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Builds the Shiprocket create order payload from your OrderEntity.
     * Handles both single-item (Buy Now) and multi-item (Cart) orders.
     * For multi-item: sums total weight, uses max dimension across items.
     */
    private ShiprocketOrderRequest buildCreateOrderRequest(OrderEntity order) {
        ShiprocketOrderRequest req = new ShiprocketOrderRequest();

        // Identity
        req.setOrderId(order.getOrderStrId());
        req.setOrderDate(order.getOrderDate().format(SR_DATE_FORMAT));
        req.setPickupLocation(pickupLocation);
        req.setComment(order.getOrderNotes());

        // Customer / Address
        req.setBillingCustomerName(order.getCustomerName());
        req.setBillingAddress(order.getShippingAddress1());
        req.setBillingAddress2(order.getShippingAddress2() != null ? order.getShippingAddress2() : "");
        req.setBillingCity(order.getShippingCity());
        req.setBillingPincode(order.getShippingPincode());
        req.setBillingState(order.getShippingState());
        req.setBillingCountry(order.getShippingCountry());
        req.setBillingEmail(order.getCustomerEmail());
        req.setBillingPhone(order.getCustomerPhone());
        req.setShippingIsBilling(true);

        // Payment
        req.setPaymentMethod(
                order.getPaymentMethod() == PaymentMethod.COD ? "COD" : "Prepaid"
        );

        // Pricing
        req.setSubTotal(order.getSubTotal());
        req.setShippingCharges(order.getShippingCharges() != null ? order.getShippingCharges() : 0.0);
        req.setGiftwrapCharges(order.getGiftwrapCharges() != null ? order.getGiftwrapCharges() : 0.0);
        req.setTransactionCharges(order.getConvenienceFee() != null ? order.getConvenienceFee() : 0.0);
        req.setTotalDiscount(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0);

        // Order Items — map each OrderItemEntity → ShiprocketOrderItem
        List<ShiprocketOrderRequest.ShiprocketOrderItem> srItems = order.getOrderItems()
                .stream()
                .map(this::mapToSrItem)
                .collect(Collectors.toList());
        req.setOrderItems(srItems);

        // Dimensions — for multi-item: sum weights, use max of each dimension
        // SR needs one shipment box size — use the largest item as the box
        double totalWeight = order.getOrderItems().stream()
                .mapToDouble(i -> i.getWeight() != null ? i.getWeight() * i.getQuantity() : 0.0)
                .sum();
        double maxLength = order.getOrderItems().stream()
                .mapToDouble(i -> i.getLength() != null ? i.getLength() : 0.0)
                .max().orElse(10.0);
        double maxBreadth = order.getOrderItems().stream()
                .mapToDouble(i -> i.getBreadth() != null ? i.getBreadth() : 0.0)
                .max().orElse(10.0);
        double maxHeight = order.getOrderItems().stream()
                .mapToDouble(i -> i.getHeight() != null ? i.getHeight() : 0.0)
                .max().orElse(10.0);

        req.setWeight(totalWeight > 0 ? totalWeight : 0.5);  // fallback 0.5kg if missing
        req.setLength(maxLength > 0 ? maxLength : 10.0);
        req.setBreadth(maxBreadth > 0 ? maxBreadth : 10.0);
        req.setHeight(maxHeight > 0 ? maxHeight : 10.0);

        return req;
    }

    /**
     * Maps a single OrderItemEntity → ShiprocketOrderItem
     */
    private ShiprocketOrderRequest.ShiprocketOrderItem mapToSrItem(OrderItemEntity item) {
        ShiprocketOrderRequest.ShiprocketOrderItem srItem =
                new ShiprocketOrderRequest.ShiprocketOrderItem();

        // Append color/size to name for variant products so SR panel shows it clearly
        String displayName = item.getProductName();
        if (item.getColor() != null && !item.getColor().isEmpty()) {
            displayName += " - " + item.getColor();
        }
        if (item.getSize() != null && !item.getSize().isEmpty()
                && !item.getSize().equalsIgnoreCase("Standard")) {
            displayName += " / " + item.getSize();
        }

        srItem.setName(displayName);
        srItem.setSku(item.getSku());
        srItem.setUnits(item.getQuantity());
        srItem.setSellingPrice(String.valueOf(item.getSellingPrice()));
        srItem.setDiscount(item.getDiscount() != null ? String.valueOf(item.getDiscount()) : "0");
        srItem.setHsn(item.getHsnCode());

        return srItem;
    }

    /**
     * Builds the Shiprocket return order payload from your OrderEntity.
     * Pickup = customer address, Shipping = your warehouse.
     */
    private ShiprocketReturnRequest buildReturnOrderRequest(OrderEntity order) {
        ShiprocketReturnRequest req = new ShiprocketReturnRequest();

        req.setOrderId("RTN-" + order.getOrderStrId());     // prefix to avoid SR duplicate order_id
        req.setOrderDate(order.getOrderDate().format(SR_DATE_FORMAT));

        // Pickup from customer
        req.setPickupCustomerName(order.getCustomerName());
        req.setPickupPhone(order.getCustomerPhone());
        req.setPickupAddress(order.getShippingAddress1());
        req.setPickupCity(order.getShippingCity());
        req.setPickupState(order.getShippingState());
        req.setPickupPincode(order.getShippingPincode());

        // Return to your warehouse
        req.setShippingCustomerName(warehouseName);
        req.setShippingPhone(warehousePhone);
        req.setShippingAddress(warehouseAddress);
        req.setShippingCity(warehouseCity);
        req.setShippingState(warehouseState);
        req.setShippingPincode(warehousePincode);

        req.setSubTotal(order.getSubTotal());

        // Dimensions — same logic as forward order
        double totalWeight = order.getOrderItems().stream()
                .mapToDouble(i -> i.getWeight() != null ? i.getWeight() * i.getQuantity() : 0.0)
                .sum();
        req.setWeight(totalWeight > 0 ? totalWeight : 0.5);
        req.setLength(order.getOrderItems().stream()
                .mapToDouble(i -> i.getLength() != null ? i.getLength() : 10.0).max().orElse(10.0));
        req.setBreadth(order.getOrderItems().stream()
                .mapToDouble(i -> i.getBreadth() != null ? i.getBreadth() : 10.0).max().orElse(10.0));
        req.setHeight(order.getOrderItems().stream()
                .mapToDouble(i -> i.getHeight() != null ? i.getHeight() : 10.0).max().orElse(10.0));

        req.setOrderItems(order.getOrderItems().stream()
                .map(this::mapToSrItem)
                .collect(Collectors.toList()));

        return req;
    }

    /**
     * Executes a Shiprocket API call with automatic token retry on 401.
     * If token is expired mid-session, invalidates cache and retries once.
     */
    private <T> T executeWithTokenRetry(java.util.function.Supplier<T> apiCall) {
        try {
            return apiCall.get();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Shiprocket returned 401 — token may be expired, refreshing and retrying...");
            authService.invalidateToken();
            return apiCall.get();  // retry once with fresh token
        }
    }
}