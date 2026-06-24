package com.artezo.service.serviceImpl;

import com.artezo.dto.request.ShiprocketOrderPayload;
import com.artezo.dto.request.ShiprocketWebhookPayload;
import com.artezo.entity.*;
import com.artezo.enum_status.*;
import com.artezo.repository.*;
import com.artezo.service.SRCheckoutService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SRCheckoutServiceImpl implements SRCheckoutService {

    private final SRPreCheckoutRepository srPreCheckoutRepository;
    private final OrderRepository         orderRepository;
    private final ProductRepository       productRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  1. PRE-CHECKOUT
    //     Frontend calls this BEFORE opening the SR window.
    //     We save a PENDING record so abandoned checkouts are tracked.
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void createPendingOrder(String orderRef, Map<String, Object> body) {

        // Idempotency: don't double-insert if frontend calls twice
        if (srPreCheckoutRepository.existsByOrderRef(orderRef)) {
            log.warn("Pre-checkout already exists for ref={}, skipping", orderRef);
            return;
        }

        SRPreCheckoutEntity pre = new SRPreCheckoutEntity();
        pre.setOrderRef(orderRef);
        pre.setStatus(SRCheckoutStatus.PENDING);

        // ── Map fields from frontend body ──
        pre.setProductStrId(safeStr(body, "productId"));
        pre.setProductName(safeStr(body, "productName"));
        pre.setVariantId(safeStr(body, "variantId"));
        pre.setVariantLabel(safeStr(body, "variantLabel"));
        pre.setSku(safeStr(body, "sku"));
        pre.setSource(safeStr(body, "source", "SR_HOT_CHECKOUT"));

        // qty / price — frontend sends these as numbers; Jackson deserialises to Integer/Double
        pre.setQuantity(safeInt(body, "qty"));
        pre.setUnitPrice(safeDouble(body, "unitPrice"));
        pre.setMrp(safeDouble(body, "mrp"));
        pre.setTotalAmount(safeDouble(body, "totalAmount"));

        srPreCheckoutRepository.save(pre);
        log.info("Pre-checkout saved: ref={}, product={}, qty={}, total={}",
                orderRef, pre.getProductStrId(), pre.getQuantity(), pre.getTotalAmount());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. PROCESS SR ORDER (Custom Endpoint — fires after payment)
    //
    //  SR POST body is deserialized into ShiprocketOrderPayload.
    //  We:
    //    a) Find the SRPreCheckoutEntity by orderRef (payload.orderName())
    //    b) Build OrderEntity from SR address + payment data
    //    c) Build OrderItemEntity from SR line items + product snapshot from DB
    //    d) Persist order, update SRPreCheckoutEntity → CONFIRMED
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void processShiprocketOrder(ShiprocketOrderPayload payload) {

        String orderRef = payload.orderName(); // SR echoes back the order_id you sent

        // ── Idempotency: if order already confirmed, skip ──
        Optional<SRPreCheckoutEntity> existingPre = srPreCheckoutRepository.findByOrderRef(orderRef);
        if (existingPre.isPresent() && existingPre.get().getStatus() == SRCheckoutStatus.CONFIRMED) {
            log.warn("SR order already processed for ref={}, srOrderId={}", orderRef, payload.srOrderId());
            return;
        }

        SRPreCheckoutEntity pre = existingPre.orElseGet(() -> {
            // SR fired without a pre-checkout record (possible if pre-checkout call failed)
            // Create a minimal stub so we can still save the order
            log.warn("No pre-checkout record found for ref={}, creating stub", orderRef);
            SRPreCheckoutEntity stub = new SRPreCheckoutEntity();
            stub.setOrderRef(orderRef);
            stub.setStatus(SRCheckoutStatus.PENDING);
            stub.setSource("SR_HOT_CHECKOUT");
            return stub;
        });

        try {
            // ── Build OrderEntity ──────────────────────────────────────────────
            OrderEntity order = new OrderEntity();

            // Flow type — marks this as a Buy Now / SR Checkout order
            order.setOrderFlow(OrderFlow.BUY_NOW);

            // Order status — starts as CONFIRMED since SR only fires after payment
            order.setOrderStatus(OrderStatus.CONFIRMED);

            // ── Payment ──
            String financial = payload.financialStatus(); // "paid" | "pending" | "cod"
            if ("paid".equalsIgnoreCase(financial)) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaymentMethod(PaymentMethod.PREPAID);
                order.setPaymentMode(resolvePaymentMode(payload.paymentGateway()));
                order.setRazorpayOrderId(payload.gatewayOrderId());
            } else if ("cod".equalsIgnoreCase(financial) || "pending".equalsIgnoreCase(financial)) {
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setPaymentMethod(PaymentMethod.COD);
                order.setPaymentMode(PaymentMode.COD);
            } else {
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setPaymentMethod(PaymentMethod.PREPAID);
            }

            // ── Customer ──
            ShiprocketOrderPayload.CustomerInfo cust = payload.customer();
            if (cust != null) {
                String fullName = trim(cust.firstName()) + " " + trim(cust.lastName());
                order.setCustomerName(fullName.trim());
                order.setCustomerEmail(cust.email());
                order.setCustomerPhone(cust.phone());
            }

            // ── Shipping address (SR gives shipping_address) ──
            ShiprocketOrderPayload.Address addr = payload.shippingAddress();
            if (addr != null) {
                order.setShippingAddress1(addr.address1());
                order.setShippingAddress2(addr.address2());
                order.setShippingCity(addr.city());
                order.setShippingState(addr.province());
                order.setShippingPincode(addr.zip());
                order.setShippingCountry(addr.country() != null ? addr.country() : "India");
            }

            // ── Pricing ──
            double total = parseAmount(payload.totalPrice());
            order.setFinalAmount(total);
            order.setSubTotal(parseAmount(payload.subtotalPrice()));
            order.setTax(parseAmount(payload.totalTax()));
            order.setDiscountAmount(parseAmount(payload.totalDiscounts()));
            order.setShippingCharges(0.0); // SR handles shipping — set 0 here

            // ── SR tracking fields ──
            // shiprocketOrderId comes as string from SR; map to Long if numeric
            order.setShiprocketOrderId(parseLong(payload.srOrderId()));
            order.setShippingStatus(ShippingStatus.NEW);

            // ── Order items ──────────────────────────────────────────────────
            if (payload.lineItems() != null) {
                for (ShiprocketOrderPayload.LineItem lineItem : payload.lineItems()) {
                    OrderItemEntity item = buildOrderItem(order, lineItem, pre);
                    order.getOrderItems().add(item);
                }
            }

            // ── Calculate subTotal from items if SR didn't provide it ──
            if (order.getSubTotal() == null || order.getSubTotal() == 0) {
                double sub = order.getOrderItems().stream()
                        .mapToDouble(i -> i.getItemTotal() != null ? i.getItemTotal() : 0.0)
                        .sum();
                order.setSubTotal(sub);
            }

            // ── Persist ──
            OrderEntity saved = orderRepository.save(order);
            log.info("OrderEntity saved: orderId={}, orderStrId={}, flow=BUY_NOW, total={}",
                    saved.getOrderId(), saved.getOrderStrId(), saved.getFinalAmount());

            // ── Update SRPreCheckoutEntity ──
            pre.setStatus(SRCheckoutStatus.CONFIRMED);
            pre.setSrOrderId(payload.srOrderId());
            pre.setOrder(saved);
            pre.setConfirmedAt(LocalDateTime.now());

            // Populate customer fields on pre-checkout for analytics
            if (cust != null) {
                pre.setCustomerName(order.getCustomerName());
                pre.setCustomerEmail(cust.email());
                pre.setCustomerPhone(cust.phone());
            }
            pre.setFinancialStatus(payload.financialStatus());
            pre.setPaymentGateway(payload.paymentGateway());
            pre.setGatewayOrderId(payload.gatewayOrderId());

            srPreCheckoutRepository.save(pre);

        } catch (Exception e) {
            // Mark as FAILED — don't rethrow so controller still returns 200 to SR
            pre.setStatus(SRCheckoutStatus.FAILED);
            srPreCheckoutRepository.save(pre);
            log.error("Failed to process SR order for ref={}: {}", orderRef, e.getMessage(), e);
            throw e; // re-throw so controller can log — controller catches and returns 200
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. WEBHOOK EVENT HANDLER
    //     Updates OrderEntity fields based on SR real-time events.
    //     Add more event types as needed.
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void handleWebhookEvent(ShiprocketWebhookPayload payload) {

        String srOrderId = payload.srOrderId();
        if (srOrderId == null) {
            log.warn("Webhook received without sr_order_id, event={}", payload.event());
            return;
        }

        // Find order by shiprocketOrderId
        Long srId = parseLong(srOrderId);
        if (srId == null) {
            log.warn("Cannot parse sr_order_id={} as Long", srOrderId);
            return;
        }

        Optional<OrderEntity> orderOpt = orderRepository.findByShiprocketOrderId(srId);
        if (orderOpt.isEmpty()) {
            // Order might not be saved yet (race condition) — safe to ignore
            log.warn("No OrderEntity found for shiprocketOrderId={}, event={}", srId, payload.event());
            return;
        }

        OrderEntity order = orderOpt.get();
        String event = payload.event();

        switch (event != null ? event.toUpperCase() : "") {

            case "ORDER_PLACED":
                order.setOrderStatus(OrderStatus.CONFIRMED);
                break;

            case "PAYMENT_SUCCESS":
                order.setPaymentStatus(PaymentStatus.PAID);
                break;

            case "PAYMENT_FAILED":
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                break;

            case "AWB_ASSIGNED":
                if (payload.awb() != null)     order.setAwbNumber(payload.awb());
                if (payload.courier() != null) order.setCourierName(payload.courier());
                order.setShippingStatus(ShippingStatus.PICKUP_SCHEDULED);
                break;

            case "PICKUP_SCHEDULED":
                order.setShippingStatus(ShippingStatus.PICKUP_SCHEDULED);
                break;

            case "IN_TRANSIT":
                order.setShippingStatus(ShippingStatus.IN_TRANSIT);
                break;

            case "OUT_FOR_DELIVERY":
                order.setShippingStatus(ShippingStatus.OUT_FOR_DELIVERY);
                break;

            case "DELIVERED":
                order.setShippingStatus(ShippingStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                order.setOrderStatus(OrderStatus.DELIVERED);
                break;

            case "FAILED_DELIVERY":
                order.setShippingStatus(ShippingStatus.FAILED_DELIVERY);
                break;

            case "CANCELLED":
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());
                break;

            default:
                log.debug("Unhandled SR webhook event={} for srOrderId={}", event, srOrderId);
        }

        orderRepository.save(order);
        log.info("Webhook processed: event={}, srOrderId={}, newStatus={}",
                event, srOrderId, order.getOrderStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a single OrderItemEntity from an SR line item.
     * Tries to resolve the ProductEntity for dimension/HSN snapshot;
     * falls back to SR-provided data if product not found.
     */
    private OrderItemEntity buildOrderItem(
            OrderEntity order,
            ShiprocketOrderPayload.LineItem lineItem,
            SRPreCheckoutEntity pre) {

        OrderItemEntity item = new OrderItemEntity();
        item.setOrder(order);
        item.setItemStatus(ItemStatus.ACTIVE);

        // ── Try to resolve product from DB for snapshot ──
        ProductEntity product = null;
        if (lineItem.sku() != null) {
            product = productRepository.findByCurrentSkuOrVariantSku(lineItem.sku()).orElse(null);
        }
        if (product == null && pre.getProductStrId() != null) {
            product = productRepository.findByProductStrId(pre.getProductStrId()).orElse(null);
        }

        if (product != null) {
            // ── Snapshot from DB (authoritative) ──
            item.setProductStrId(product.getProductStrId());
            item.setProductName(product.getProductName());
            item.setBrandName(product.getBrandName());
            item.setHsnCode(product.getHsnCode());
            item.setWeight(product.getWeight());
            item.setLength(product.getLength());
            item.setBreadth(product.getBreadth());
            item.setHeight(product.getHeight());

            // Resolve variant if variantId is known
            if (pre.getVariantId() != null && product.getVariants() != null) {
                product.getVariants().stream()
                        .filter(v -> pre.getVariantId().equals(v.getVariantId()))
                        .findFirst()
                        .ifPresent(v -> {
                            item.setVariantId(v.getVariantId());
                            item.setColor(v.getColor());
                            item.setSize(v.getSize());
                            item.setSku(v.getSku());
                            // Variant-level dimensions override product-level if present
                            if (v.getWeight() != null) item.setWeight(v.getWeight());
                            if (v.getLength() != null) item.setLength(v.getLength());
                            if (v.getBreadth() != null) item.setBreadth(v.getBreadth());
                            if (v.getHeight() != null) item.setHeight(v.getHeight());
                        });
            }
        } else {
            // ── Fallback: use SR + pre-checkout data ──
            log.warn("Product not found in DB for sku={}, using SR data", lineItem.sku());
            item.setProductStrId(pre.getProductStrId());
            item.setProductName(lineItem.title());
            item.setSku(lineItem.sku());
            item.setVariantId(pre.getVariantId());
        }

        // ── Item name: use pre-checkout's full display name (includes variant label) ──
        if (pre.getProductName() != null) {
            item.setProductName(pre.getProductName());
        }

        // ── Quantity & pricing ──
        int qty = lineItem.quantity() != null ? lineItem.quantity() : 1;
        double price = parseAmount(lineItem.price());

        item.setQuantity(qty);
        item.setSellingPrice(price);
        item.setMrpPrice(pre.getMrp() != null ? pre.getMrp() : price);
        item.setDiscount(item.getMrpPrice() - price);
        item.setItemTotal(price * qty);

        return item;
    }

    /** Maps SR paymentGateway string to your PaymentMode enum */
    private PaymentMode resolvePaymentMode(String gateway) {
        if (gateway == null) return PaymentMode.RAZORPAY;
        return switch (gateway.toLowerCase()) {
            case "upi"         -> PaymentMode.UPI;
            case "netbanking"  -> PaymentMode.NETBANKING;
            case "card"        -> PaymentMode.CARD;
            case "cod"         -> PaymentMode.COD;
            default            -> PaymentMode.RAZORPAY;
        };
    }

    // ── Safe parse helpers ────────────────────────────────────────────────────

    private String safeStr(Map<String, Object> map, String key) {
        return safeStr(map, key, null);
    }

    private String safeStr(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val != null ? val.toString() : fallback;
    }

    private Integer safeInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Integer i) return i;
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private Double safeDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Double d) return d;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private double parseAmount(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return null; }
    }

    private String trim(String s) {
        return s != null ? s.trim() : "";
    }
}