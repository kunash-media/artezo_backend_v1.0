package com.artezo.util;

import com.artezo.dto.request.ShiprocketOrderPayload;
import com.artezo.dto.request.ShiprocketWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderCheckoutRepository orderCheckoutRepository;

    /**
     * Called by the SR Custom Endpoint after order placement.
     * Upserts the order (in case pre-checkout already created a PENDING record).
     */
    @Transactional
    public void processShiprocketOrder(ShiprocketOrderPayload payload) {
        // Try to find existing PENDING order (from pre-checkout)
        Order order = orderCheckoutRepository.findByOrderRef(payload.orderName())
                .orElse(new Order());

        // Map SR payload → your entity
        order.setSrOrderId(payload.srOrderId());
        order.setOrderRef(payload.orderName());
        order.setCustomerName(payload.customer().firstName() + " " + payload.customer().lastName());
        order.setCustomerEmail(payload.customer().email());
        order.setCustomerPhone(payload.customer().phone());

        if (payload.shippingAddress() != null) {
            order.setShippingAddress(formatAddress(payload.shippingAddress()));
            order.setShippingCity(payload.shippingAddress().city());
            order.setShippingPincode(payload.shippingAddress().zip());
            order.setShippingState(payload.shippingAddress().province());
        }

        order.setTotalAmount(new BigDecimal(payload.totalPrice() != null ? payload.totalPrice() : "0"));
        order.setPaymentMethod(payload.paymentGateway());
        order.setRazorpayOrderId(payload.gatewayOrderId());

        // Map financial status to your order status
        order.setStatus(mapFinancialStatus(payload.financialStatus()));

        // Store raw line items as JSON or map to your product entities
        if (payload.lineItems() != null && !payload.lineItems().isEmpty()) {
            var firstItem = payload.lineItems().get(0);
            order.setProductName(firstItem.title());
            order.setProductSku(firstItem.sku());
            order.setQuantity(firstItem.quantity());
        }

        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setSource("SHIPROCKET_CHECKOUT");

        orderCheckoutRepository.save(order);
        log.info("Order saved: id={}, ref={}, status={}", order.getId(), order.getOrderRef(), order.getStatus());
    }

    /**
     * Called by the SR Webhook for order status events.
     */
    @Transactional
    public void handleWebhookEvent(ShiprocketWebhookPayload payload) {
        Optional<Order> optOrder = orderCheckoutRepository.findBySrOrderId(payload.srOrderId());

        if (optOrder.isEmpty()) {
            log.warn("Webhook for unknown SR order {}", payload.srOrderId());
            return;
        }

        Order order = optOrder.get();

        switch (payload.event() != null ? payload.event() : "") {
            case "PAYMENT_SUCCESS" -> order.setStatus(OrderStatus.PAID);
            case "ORDER_PLACED"    -> order.setStatus(OrderStatus.CONFIRMED);
            case "SHIPPED"         -> {
                order.setStatus(OrderStatus.SHIPPED);
                order.setTrackingAwb(payload.awb());
                order.setCourierName(payload.courier());
            }
            case "DELIVERED"       -> order.setStatus(OrderStatus.DELIVERED);
            case "CANCELLED"       -> order.setStatus(OrderStatus.CANCELLED);
            default -> log.info("Unhandled SR event: {}", payload.event());
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderCheckoutRepository.save(order);
    }

    /**
     * Called BEFORE checkout is opened — creates a PENDING stub.
     */
    @Transactional
    public void createPendingOrder(String orderRef, Map<String, Object> data) {
        // Avoid duplicates if user clicks Buy Now twice
        if (orderCheckoutRepository.findByOrderRef(orderRef).isPresent()) return;

        Order order = new Order();
        order.setOrderRef(orderRef);
        order.setStatus(OrderStatus.PENDING);
        order.setProductName((String) data.getOrDefault("productName", ""));
        order.setQuantity(((Number) data.getOrDefault("qty", 1)).intValue());
        order.setTotalAmount(new BigDecimal(data.getOrDefault("total", "0").toString()));
        order.setSource("SHIPROCKET_CHECKOUT");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderCheckoutRepository.save(order);
    }

    private OrderStatus mapFinancialStatus(String srStatus) {
        if (srStatus == null) return OrderStatus.PENDING;
        return switch (srStatus.toLowerCase()) {
            case "paid"    -> OrderStatus.PAID;
            case "pending" -> OrderStatus.PENDING;
            case "cod"     -> OrderStatus.CONFIRMED; // COD confirmed but not yet paid
            default        -> OrderStatus.PENDING;
        };
    }

    private String formatAddress(ShiprocketOrderPayload.Address a) {
        return String.join(", ",
                Optional.ofNullable(a.address1()).orElse(""),
                Optional.ofNullable(a.address2()).orElse(""),
                Optional.ofNullable(a.city()).orElse("")
        );
    }
}