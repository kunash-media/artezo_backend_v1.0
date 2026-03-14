package com.artezo.controller;

import com.artezo.entity.OrderEntity;
import com.artezo.entity.OrderItemEntity;
import com.artezo.entity.ProductEntity;
import com.artezo.enum_status.ItemStatus;
import com.artezo.enum_status.OrderStatus;
import com.artezo.enum_status.ShippingStatus;
import com.artezo.repository.OrderRepository;
import com.artezo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class ShiprocketWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ShiprocketWebhookController.class);

    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;

    public ShiprocketWebhookController(OrderRepository orderRepository,
                                       ProductRepository productRepository) {
        this.orderRepository   = orderRepository;
        this.productRepository = productRepository;
    }

    @PostMapping("/shiprocket")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Shiprocket webhook received: {}", payload);

        try {
            String orderStrId    = (String) payload.get("order_id");
            String currentStatus = (String) payload.get("current_status");
            String awb           = (String) payload.get("awb");
            String courierName   = (String) payload.get("courier_name");

            if (orderStrId == null || currentStatus == null) {
                log.warn("Webhook missing required fields — ignoring");
                return ResponseEntity.ok("ignored");
            }

            OrderEntity order = orderRepository.findByOrderStrId(orderStrId).orElse(null);
            if (order == null) {
                log.warn("Webhook: order not found for orderStrId: {}", orderStrId);
                return ResponseEntity.ok("order not found");
            }

            // ── Update AWB + courier ──────────────────────────────────────
            if (awb != null && !awb.isEmpty()) {
                order.setAwbNumber(awb);
            }
            if (courierName != null && !courierName.isEmpty()) {
                order.setCourierName(courierName);
            }

            // ── Map SR status → your ShippingStatus enum ──────────────────
            order.setShippingStatus(mapShippingStatus(currentStatus));

            // ── DELIVERED ─────────────────────────────────────────────────
            // Stock already decreased on order confirm — no stock change here
            if ("Delivered".equalsIgnoreCase(currentStatus)) {
                order.setOrderStatus(OrderStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                log.info("Order {} marked DELIVERED", orderStrId);
            }

            // ── RTO / RETURNED TO ORIGIN ───────────────────────────────────
            // Item physically back in your warehouse → increase stock back
            if ("RTO Delivered".equalsIgnoreCase(currentStatus)
                    || "Returned to Origin".equalsIgnoreCase(currentStatus)) {
                order.setOrderStatus(OrderStatus.RETURNED);
                order.getOrderItems().forEach(i -> i.setItemStatus(ItemStatus.RETURNED));

                // ✅ Increase stock — item is back in warehouse
                increaseStock(order.getOrderItems());
                log.info("Order {} — RTO received, stock restored", orderStrId);
            }

            // ── RETURN INITIATED / RTO INITIATED ──────────────────────────
            // Item picked up from customer but not yet back — don't touch stock yet
            if ("Return Initiated".equalsIgnoreCase(currentStatus)
                    || "RTO Initiated".equalsIgnoreCase(currentStatus)) {
                order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
                log.info("Order {} — return initiated, awaiting warehouse receipt", orderStrId);
            }

            // ── CANCELLED by SR (e.g. courier cancelled) ──────────────────
            // Increase stock back — item was never shipped
            if ("Cancelled".equalsIgnoreCase(currentStatus)) {
                if (order.getOrderStatus() != OrderStatus.CANCELLED) {
                    // Only increase if not already cancelled via your cancel endpoint
                    // (your cancelOrder() already called increaseStock)
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    order.setCancelledAt(LocalDateTime.now());
                    increaseStock(order.getOrderItems());
                    log.info("Order {} — cancelled by SR, stock restored", orderStrId);
                }
            }

            orderRepository.save(order);
            log.info("Order {} updated — status: {}, AWB: {}", orderStrId, currentStatus, awb);

        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            // Always return 200 to SR — otherwise SR will keep retrying
        }

        return ResponseEntity.ok("received");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STOCK HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Increases stock for each item — called on:
     *   - RTO Delivered  (return physically back in warehouse)
     *   - SR Cancelled   (shipment cancelled before dispatch)
     */
    private void increaseStock(List<OrderItemEntity> items) {
        for (OrderItemEntity item : items) {
            ProductEntity product = productRepository
                    .findByProductStrId(item.getProductStrId())
                    .orElse(null);

            if (product == null) {
                log.warn("increaseStock — product not found: {}", item.getProductStrId());
                continue;
            }

            if (product.getHasVariants() && item.getVariantId() != null) {
                // ── Variant level stock increase ──────────────────────────
                product.getVariants().stream()
                        .filter(v -> v.getVariantId().equals(item.getVariantId()))
                        .findFirst()
                        .ifPresentOrElse(
                                v -> {
                                    v.setStock(v.getStock() + item.getQuantity());
                                    log.info("Stock +{} → variant: {} (now: {})",
                                            item.getQuantity(), v.getVariantId(), v.getStock());
                                },
                                () -> log.warn("increaseStock — variant not found: {}", item.getVariantId())
                        );
            } else {
                // ── Root product stock increase ───────────────────────────
                int updated = product.getCurrentStock() + item.getQuantity();
                product.setCurrentStock(updated);
                log.info("Stock +{} → product: {} (now: {})",
                        item.getQuantity(), product.getProductStrId(), updated);
            }

            productRepository.save(product);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STATUS MAPPER
    // ════════════════════════════════════════════════════════════════════════

    private ShippingStatus mapShippingStatus(String srStatus) {
        if (srStatus == null) return ShippingStatus.NEW;
        return switch (srStatus.toLowerCase()) {
            case "new"                           -> ShippingStatus.NEW;
            case "pickup scheduled",
                 "pickup generated"              -> ShippingStatus.PICKUP_SCHEDULED;
            case "picked up"                     -> ShippingStatus.PICKED_UP;
            case "in transit"                    -> ShippingStatus.IN_TRANSIT;
            case "out for delivery"              -> ShippingStatus.OUT_FOR_DELIVERY;
            case "delivered"                     -> ShippingStatus.DELIVERED;
            case "failed delivery",
                 "undelivered"                   -> ShippingStatus.FAILED_DELIVERY;
            case "cancelled"                     -> ShippingStatus.CANCELLED;
            case "rto initiated",
                 "return initiated"              -> ShippingStatus.RETURN_INITIATED;
            case "rto delivered",
                 "returned to origin"            -> ShippingStatus.RETURNED_TO_ORIGIN;
            default                              -> ShippingStatus.IN_TRANSIT;
        };
    }
}