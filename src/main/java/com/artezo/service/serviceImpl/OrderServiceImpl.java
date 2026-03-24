package com.artezo.service.serviceImpl;

import com.artezo.dto.request.BuyNowConfirmRequest;
import com.artezo.dto.request.CreateOrderRequest;
import com.artezo.dto.response.OrderResponse;
import com.artezo.dto.response.ShiprocketOrderResponse;
import com.artezo.entity.*;
import com.artezo.enum_status.*;
import com.artezo.exceptions.OrderException;
import com.artezo.repository.OrderRepository;
import com.artezo.repository.ProductRepository;
import com.artezo.repository.UserRepository;
import com.artezo.service.EmailService;
import com.artezo.service.OrderService;
import com.artezo.service.ShiprocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);


    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;
    private final ShiprocketService shiprocketService;
    private final EmailService emailService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            ShiprocketService shiprocketService, EmailService emailService) {
        this.orderRepository   = orderRepository;
        this.productRepository = productRepository;
        this.userRepository    = userRepository;
        this.shiprocketService = shiprocketService;
        this.emailService = emailService;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CREATE ORDER — Cart Flow
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order for userId: {}", userId);

        // 1. Fetch user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("User not found", "USER_NOT_FOUND"));

        // 2. Build order items + validate stock
        List<OrderItemEntity> orderItems = new ArrayList<>();
        double subTotal = 0.0;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductEntity product = productRepository.findByProductStrId(itemReq.getProductStrId())
                    .orElseThrow(() -> new OrderException(
                            "Product not found: " + itemReq.getProductStrId(), "PRODUCT_NOT_FOUND"));
            OrderItemEntity item = buildOrderItem(product, itemReq);
            orderItems.add(item);
            subTotal += item.getItemTotal();
        }

        // 3. Calculate final amount
        double discountAmount  = orZero(request.getDiscountAmount());
        double couponDiscount  = orZero(request.getCouponDiscount());
        double tax             = orZero(request.getTax());
        double convenienceFee  = orZero(request.getConvenienceFee());
        double shippingCharges = orZero(request.getShippingCharges());
        double giftwrapCharges = request.isGiftWrap() ? orZero(request.getGiftwrapCharges()) : 0.0;

        double finalAmount = subTotal - discountAmount - couponDiscount
                + tax + convenienceFee + shippingCharges + giftwrapCharges;

        // 4. Build OrderEntity
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
        order.setPaymentMode(request.getPaymentMode() != null
                ? PaymentMode.valueOf(request.getPaymentMode()) : null);
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpayOrderId(request.getRazorpayOrderId());

        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setShippingAddress1(request.getShippingAddress1());
        order.setShippingAddress2(request.getShippingAddress2());
        order.setShippingCity(request.getShippingCity());
        order.setShippingState(request.getShippingState());
        order.setShippingPincode(request.getShippingPincode());
        order.setShippingCountry("India");

        order.setSubTotal(subTotal);
        order.setDiscountAmount(discountAmount);
        order.setDiscountPercent(request.getDiscountPercent());
        order.setCouponCode(request.getCouponCode());
        order.setCouponDiscount(couponDiscount);
        order.setTax(tax);
        order.setConvenienceFee(convenienceFee);
        order.setShippingCharges(shippingCharges);
        order.setGiftWrap(request.isGiftWrap());
        order.setGiftwrapCharges(giftwrapCharges);
        order.setFinalAmount(finalAmount);
        order.setOrderNotes(request.getOrderNotes());
        order.setOrderFlow(OrderFlow.CART);
        order.setShippingStatus(ShippingStatus.NEW);

        for (OrderItemEntity item : orderItems) {
            item.setOrder(order);
        }
        order.setOrderItems(orderItems);

        // 5. Save to DB — PENDING
        OrderEntity savedOrder = orderRepository.save(order);
        log.info("Order saved to DB: {} — calling Shiprocket...", savedOrder.getOrderStrId());

        // 6. Call Shiprocket + Send Email on Success
        try {
            ShiprocketOrderResponse srResponse = shiprocketService.createOrder(savedOrder);

            // Update order with Shiprocket details
            savedOrder.setShiprocketOrderId(srResponse.getOrderId());
            savedOrder.setShiprocketShipmentId(srResponse.getShipmentId());
            savedOrder.setOrderStatus(OrderStatus.CONFIRMED);

            if (srResponse.getAwbCode() != null && !srResponse.getAwbCode().isEmpty()) {
                savedOrder.setAwbNumber(srResponse.getAwbCode());
            }
            if (srResponse.getCourierName() != null) {
                savedOrder.setCourierName(srResponse.getCourierName());
            }

            if (request.getRazorpayPaymentId() != null) {
                savedOrder.setPaymentStatus(PaymentStatus.PAID);
            }

            decreaseStock(savedOrder.getOrderItems());
            orderRepository.save(savedOrder);

            log.info("Order {} confirmed — SR Order ID: {}", savedOrder.getOrderStrId(), srResponse.getOrderId());

            // ==================== SEND EMAIL ON SUCCESS ====================
            sendOrderConfirmationEmail(savedOrder);

        } catch (Exception e) {
            log.error("Shiprocket createOrder failed for {} — saved as PENDING: {}",
                    savedOrder.getOrderStrId(), e.getMessage());

            savedOrder.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(savedOrder);
        }

        return mapToOrderResponse(savedOrder);
    }


    private void sendOrderConfirmationEmail(OrderEntity savedOrder) {
        try {
            String customerEmail = savedOrder.getCustomerEmail();
            String customerName  = savedOrder.getCustomerName();
            String orderStrId    = savedOrder.getOrderStrId();
            String mobile        = savedOrder.getCustomerPhone();

            BigDecimal totalAmount = BigDecimal.valueOf(savedOrder.getFinalAmount());

            // Convert entities to DTOs for email
            List<OrderResponse.OrderItemResponse> orderItemsForEmail = savedOrder.getOrderItems().stream()
                    .map(this::mapToOrderItemResponse)
                    .collect(Collectors.toList());

            emailService.sendOrderConfirmationEmail(
                    customerEmail,
                    customerName,
                    orderStrId,
                    totalAmount,
                    orderItemsForEmail,
                    mobile
            );

            log.info("Order confirmation email sent successfully to: {}", customerEmail);

        } catch (Exception emailEx) {
            log.error("Failed to send order confirmation email for order {}: {}",
                    savedOrder.getOrderStrId(), emailEx.getMessage(), emailEx);
            // Do NOT throw — email failure should not rollback the order
        }
    }

    //Mail order item response
    private OrderResponse.OrderItemResponse mapToOrderItemResponse(OrderItemEntity entity) {
        OrderResponse.OrderItemResponse dto = new OrderResponse.OrderItemResponse();

        dto.setProductName(entity.getProductName());
        dto.setQuantity(entity.getQuantity());
        dto.setSellingPrice(entity.getSellingPrice());
        dto.setItemTotal(entity.getItemTotal());
        // Add other fields if needed (color, sku, etc.)

        return dto;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CONFIRM BUY NOW ORDER — Buy Now Flow
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse confirmBuyNowOrder(Long userId, BuyNowConfirmRequest request) {
        log.info("Confirming Buy Now order for userId: {}, SR Order: {}",
                userId, request.getShiprocketOrderId());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("User not found", "USER_NOT_FOUND"));

        ProductEntity product = productRepository.findByProductStrId(request.getProductStrId())
                .orElseThrow(() -> new OrderException(
                        "Product not found: " + request.getProductStrId(), "PRODUCT_NOT_FOUND"));

        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setProductStrId(request.getProductStrId());
        itemReq.setVariantId(request.getVariantId());
        itemReq.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);

        OrderItemEntity item = buildOrderItem(product, itemReq);

        OrderEntity order = new OrderEntity();
//        order.setOrderStrId(generateOrderStrId());
        order.setUser(user);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(PaymentMethod.PREPAID);
        order.setPaymentMode(PaymentMode.RAZORPAY);
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setOrderFlow(OrderFlow.BUY_NOW);
        order.setShippingStatus(ShippingStatus.NEW);

        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setShippingAddress1(request.getShippingAddress1());
        order.setShippingAddress2(request.getShippingAddress2());
        order.setShippingCity(request.getShippingCity());
        order.setShippingState(request.getShippingState());
        order.setShippingPincode(request.getShippingPincode());
        order.setShippingCountry("India");

        order.setSubTotal(item.getItemTotal());
        order.setFinalAmount(request.getAmount());

        order.setShiprocketOrderId(request.getShiprocketOrderId() != null
                ? Long.parseLong(request.getShiprocketOrderId()) : null);
        order.setShiprocketShipmentId(request.getShiprocketShipmentId() != null
                ? Long.parseLong(request.getShiprocketShipmentId()) : null);

        item.setOrder(order);
        order.setOrderItems(List.of(item));

        decreaseStock(List.of(item));

        OrderEntity saved = orderRepository.save(order);
        log.info("Buy Now order saved: {}", saved.getOrderStrId());

        return mapToOrderResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CANCEL ORDER
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, String orderStrId) {
        OrderEntity order = getOrderAndValidateOwner(userId, orderStrId);

        if (order.getOrderStatus() == OrderStatus.DELIVERED
                || order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new OrderException(
                    "Order cannot be cancelled in current status: " + order.getOrderStatus(),
                    "INVALID_STATUS_FOR_CANCEL");
        }

        if (order.getShiprocketOrderId() != null) {
            try {
                shiprocketService.cancelOrder(order.getShiprocketOrderId());
            } catch (Exception e) {
                log.warn("SR cancel failed for {} — still cancelling in DB: {}",
                        orderStrId, e.getMessage());
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setShippingStatus(ShippingStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        increaseStock(order.getOrderItems());
        orderRepository.save(order);

        log.info("Order {} cancelled", orderStrId);
        return mapToOrderResponse(order);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  RETURN ORDER
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse requestReturn(Long userId, String orderStrId, String returnReason) {
        OrderEntity order = getOrderAndValidateOwner(userId, orderStrId);

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new OrderException(
                    "Return can only be requested for delivered orders",
                    "INVALID_STATUS_FOR_RETURN");
        }
        if (order.isReturnRequested()) {
            throw new OrderException(
                    "Return already requested for this order",
                    "RETURN_ALREADY_REQUESTED");
        }

        boolean anyNonReturnable = order.getOrderItems().stream()
                .anyMatch(i -> {
                    ProductEntity p = productRepository
                            .findByProductStrId(i.getProductStrId()).orElse(null);
                    return p != null && !p.getReturnAvailable();
                });
        if (anyNonReturnable) {
            throw new OrderException(
                    "One or more items in this order are not returnable",
                    "ITEM_NOT_RETURNABLE");
        }

        try {
            ShiprocketOrderResponse srResponse = shiprocketService.createReturnOrder(order);
            order.setReturnShiprocketOrderId(srResponse.getOrderId());
        } catch (Exception e) {
            log.warn("SR return creation failed for {} — marking in DB only: {}",
                    orderStrId, e.getMessage());
        }

        order.setReturnRequested(true);
        order.setReturnReason(returnReason);
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        order.setShippingStatus(ShippingStatus.RETURN_INITIATED);
        order.setReturnRequestedAt(LocalDateTime.now());
        order.getOrderItems().forEach(i -> i.setItemStatus(ItemStatus.RETURN_REQUESTED));

        orderRepository.save(order);
        log.info("Return requested for order {}", orderStrId);
        return mapToOrderResponse(order);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  EXCHANGE ORDER
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse requestExchange(Long userId, String orderStrId, String exchangeReason) {
        OrderEntity order = getOrderAndValidateOwner(userId, orderStrId);

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new OrderException(
                    "Exchange can only be requested for delivered orders",
                    "INVALID_STATUS_FOR_EXCHANGE");
        }
        if (order.isExchangeRequested()) {
            throw new OrderException(
                    "Exchange already requested for this order",
                    "EXCHANGE_ALREADY_REQUESTED");
        }

        boolean anyNonExchangeable = order.getOrderItems().stream()
                .anyMatch(i -> {
                    ProductEntity p = productRepository
                            .findByProductStrId(i.getProductStrId()).orElse(null);
                    return p != null && !p.getIsExchange();
                });
        if (anyNonExchangeable) {
            throw new OrderException(
                    "One or more items in this order are not exchangeable",
                    "ITEM_NOT_EXCHANGEABLE");
        }

        try {
            ShiprocketOrderResponse[] responses =
                    shiprocketService.exchangeOrder(order, order);
            order.setReturnShiprocketOrderId(responses[0].getOrderId());
            order.setExchangeShiprocketOrderId(responses[1].getOrderId());
        } catch (Exception e) {
            log.warn("SR exchange failed for {} — marking in DB only: {}",
                    orderStrId, e.getMessage());
        }

        order.setExchangeRequested(true);
        order.setExchangeReason(exchangeReason);
        order.setOrderStatus(OrderStatus.EXCHANGE_REQUESTED);
        order.setExchangeRequestedAt(LocalDateTime.now());
        order.getOrderItems().forEach(i -> i.setItemStatus(ItemStatus.EXCHANGE_REQUESTED));

        orderRepository.save(order);
        log.info("Exchange requested for order {}", orderStrId);
        return mapToOrderResponse(order);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  TRACK ORDER
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> trackOrder(Long userId, String orderStrId) {
        OrderEntity order = getOrderAndValidateOwner(userId, orderStrId);

        if (order.getAwbNumber() == null || order.getAwbNumber().isEmpty()) {
            throw new OrderException(
                    "Tracking not available yet — AWB not assigned",
                    "AWB_NOT_ASSIGNED");
        }

        return shiprocketService.trackByAwb(order.getAwbNumber());
    }

    // ────────────────────────────────────────────────────────────────────────
    //  GET ORDER BY ID
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, String orderStrId) {
        OrderEntity order = getOrderAndValidateOwner(userId, orderStrId);
        return mapToOrderResponse(order);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  GET ALL ORDERS FOR USER — My Orders page (user facing)
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository
                .findByUserUserIdOrderByOrderDateDesc(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  GET ALL ORDERS OF ONE USER — Admin: customer detail view
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUserId(Long userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new OrderException("User not found: " + userId, "USER_NOT_FOUND");
        }
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository
                .findByUserUserIdOrderByOrderDateDesc(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  GET ALL ORDERS — Admin: orders table (all users)
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository
                .findAllByOrderByOrderDateDesc(pageable)
                .map(this::mapToOrderResponse);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private OrderItemEntity buildOrderItem(ProductEntity product,
                                           CreateOrderRequest.OrderItemRequest itemReq) {
        OrderItemEntity item = new OrderItemEntity();
        item.setProductStrId(product.getProductStrId());
        item.setProductName(product.getProductName());
        item.setBrandName(product.getBrandName());
        item.setHsnCode(product.getHsnCode());
        item.setItemStatus(ItemStatus.ACTIVE);
        item.setQuantity(itemReq.getQuantity());

        if (product.getHasVariants() && itemReq.getVariantId() != null) {
            ProductVariantEntity variant = product.getVariants().stream()
                    .filter(v -> v.getVariantId().equals(itemReq.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new OrderException(
                            "Variant not found: " + itemReq.getVariantId(), "VARIANT_NOT_FOUND"));

            if (variant.getStock() < itemReq.getQuantity()) {
                throw new OrderException(
                        "Insufficient stock for variant: " + variant.getVariantId(),
                        "INSUFFICIENT_STOCK");
            }

            item.setVariantId(variant.getVariantId());
            item.setSku(variant.getSku());
            item.setColor(variant.getColor());
            item.setSize(variant.getSize());
            item.setMrpPrice(variant.getMrp());
            item.setSellingPrice(variant.getPrice());
            item.setWeight(variant.getWeight());
            item.setLength(variant.getLength());
            item.setBreadth(variant.getBreadth());
            item.setHeight(variant.getHeight());

        } else {
            if (product.getCurrentStock() < itemReq.getQuantity()) {
                throw new OrderException(
                        "Insufficient stock for product: " + product.getProductStrId(),
                        "INSUFFICIENT_STOCK");
            }

            item.setSku(product.getCurrentSku());
            item.setColor(product.getSelectedColor());
            item.setMrpPrice(product.getCurrentMrpPrice());
            item.setSellingPrice(product.getCurrentSellingPrice());
            item.setWeight(product.getWeight());
            item.setLength(product.getLength());
            item.setBreadth(product.getBreadth());
            item.setHeight(product.getHeight());
        }

        double discount = (item.getMrpPrice() != null && item.getSellingPrice() != null)
                ? item.getMrpPrice() - item.getSellingPrice() : 0.0;
        item.setDiscount(discount);
        item.setItemTotal(item.getSellingPrice() * itemReq.getQuantity());

        return item;
    }

    private OrderEntity getOrderAndValidateOwner(Long userId, String orderStrId) {
        OrderEntity order = orderRepository.findByOrderStrId(orderStrId)
                .orElseThrow(() -> new OrderException(
                        "Order not found: " + orderStrId, "ORDER_NOT_FOUND"));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new OrderException(
                    "Access denied to order: " + orderStrId, "ACCESS_DENIED");
        }
        return order;
    }



    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }

    private OrderResponse mapToOrderResponse(OrderEntity order) {
        OrderResponse res = new OrderResponse();
        res.setOrderStrId(order.getOrderStrId());
        res.setOrderDate(order.getOrderDate());
        res.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        res.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        res.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        res.setPaymentMode(order.getPaymentMode() != null ? order.getPaymentMode().name() : null);
        res.setOrderFlow(order.getOrderFlow() != null ? order.getOrderFlow().name() : null);
        res.setCustomerName(order.getCustomerName());
        res.setCustomerPhone(order.getCustomerPhone());
        res.setCustomerEmail(order.getCustomerEmail());
        res.setShippingAddress1(order.getShippingAddress1());
        res.setShippingAddress2(order.getShippingAddress2());
        res.setShippingCity(order.getShippingCity());
        res.setShippingState(order.getShippingState());
        res.setShippingPincode(order.getShippingPincode());
        res.setSubTotal(order.getSubTotal());
        res.setDiscountAmount(order.getDiscountAmount());
        res.setDiscountPercent(order.getDiscountPercent());
        res.setCouponCode(order.getCouponCode());
        res.setCouponDiscount(order.getCouponDiscount());
        res.setTax(order.getTax());
        res.setConvenienceFee(order.getConvenienceFee());
        res.setShippingCharges(order.getShippingCharges());
        res.setGiftwrapCharges(order.getGiftwrapCharges());
        res.setFinalAmount(order.getFinalAmount());
        res.setGiftWrap(order.isGiftWrap());
        res.setOrderNotes(order.getOrderNotes());
        res.setShiprocketOrderId(order.getShiprocketOrderId());
        res.setAwbNumber(order.getAwbNumber());
        res.setCourierName(order.getCourierName());
        res.setShippingStatus(order.getShippingStatus() != null ? order.getShippingStatus().name() : null);
        res.setReturnRequested(order.isReturnRequested());
        res.setExchangeRequested(order.isExchangeRequested());
        res.setReturnReason(order.getReturnReason());
        res.setExchangeReason(order.getExchangeReason());
        res.setCancelledAt(order.getCancelledAt());
        res.setDeliveredAt(order.getDeliveredAt());

        if (order.getOrderItems() != null) {
            List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems()
                    .stream().map(i -> {
                        OrderResponse.OrderItemResponse ir = new OrderResponse.OrderItemResponse();
                        ir.setProductStrId(i.getProductStrId());
                        ir.setProductName(i.getProductName());
                        ir.setSku(i.getSku());
                        ir.setVariantId(i.getVariantId());
                        ir.setColor(i.getColor());
                        ir.setSize(i.getSize());
                        ir.setQuantity(i.getQuantity());
                        ir.setSellingPrice(i.getSellingPrice());
                        ir.setMrpPrice(i.getMrpPrice());
                        ir.setDiscount(i.getDiscount());
                        ir.setItemTotal(i.getItemTotal());
                        ir.setItemStatus(i.getItemStatus() != null ? i.getItemStatus().name() : null);

                        // ── PRODUCT IMAGE URL ─────────────────────────────────────────
                        if (i.getProductStrId() != null) {
                            productRepository.findByProductStrId(i.getProductStrId())
                                    .ifPresent(product -> {
                                        Long primeId = product.getProductPrimeId();
                                        String variantId = i.getVariantId();

                                        if (variantId != null && !variantId.isBlank()) {
                                            // variant order — check if variant has main image
                                            boolean variantHasImage = product.getVariants().stream()
                                                    .filter(v -> variantId.equals(v.getVariantId()))
                                                    .findFirst()
                                                    .map(v -> v.getMainImageData() != null && v.getMainImageData().length > 0)
                                                    .orElse(false);

                                            if (variantHasImage) {
                                                ir.setProductImageUrl("/api/products/" + primeId + "/variant/" + variantId + "/main");
                                            } else {
                                                // fallback to product main image
                                                ir.setProductImageUrl("/api/products/" + primeId + "/main");
                                            }
                                        } else {
                                            // non-variant order — use product main image
                                            ir.setProductImageUrl("/api/products/" + primeId + "/main");
                                        }
                                    });
                        }
                        // ─────────────────────────────────────────────────────────────

                        return ir;
                    }).collect(Collectors.toList());
            res.setOrderItems(itemResponses);
        }
        return res;
    }


    // ── DECREASE STOCK — called on order confirm ──────────────────────────────
    private void decreaseStock(List<OrderItemEntity> items) {
        for (OrderItemEntity item : items) {
            ProductEntity product = productRepository
                    .findByProductStrId(item.getProductStrId())
                    .orElse(null);
            if (product == null) continue;

            if (product.getHasVariants() && item.getVariantId() != null) {
                // Decrease variant stock
                product.getVariants().stream()
                        .filter(v -> v.getVariantId().equals(item.getVariantId()))
                        .findFirst()
                        .ifPresent(v -> {
                            int updated = v.getStock() - item.getQuantity();
                            if (updated < 0) updated = 0;  // safety floor
                            v.setStock(updated);
                        });
            } else {
                // Decrease root product stock
                int updated = product.getCurrentStock() - item.getQuantity();
                if (updated < 0) updated = 0;  // safety floor
                product.setCurrentStock(updated);
            }

            productRepository.save(product);
            log.info("Stock decreased — product: {}, variant: {}, qty: {}",
                    item.getProductStrId(), item.getVariantId(), item.getQuantity());
        }
    }

    // ── INCREASE STOCK — called on cancel / return received ───────────────────
    private void increaseStock(List<OrderItemEntity> items) {
        for (OrderItemEntity item : items) {
            ProductEntity product = productRepository
                    .findByProductStrId(item.getProductStrId())
                    .orElse(null);
            if (product == null) continue;

            if (product.getHasVariants() && item.getVariantId() != null) {
                // Increase variant stock
                product.getVariants().stream()
                        .filter(v -> v.getVariantId().equals(item.getVariantId()))
                        .findFirst()
                        .ifPresent(v -> v.setStock(v.getStock() + item.getQuantity()));
            } else {
                // Increase root product stock
                product.setCurrentStock(product.getCurrentStock() + item.getQuantity());
            }

            productRepository.save(product);
            log.info("Stock increased — product: {}, variant: {}, qty: {}",
                    item.getProductStrId(), item.getVariantId(), item.getQuantity());
        }
    }
}
