package com.artezo.service.serviceImpl;

import com.artezo.dto.request.BuyNowConfirmRequest;
import com.artezo.dto.request.CreateOrderRequest;
import com.artezo.dto.request.MagicCheckoutConfirmRequest;
import com.artezo.dto.response.OrderResponse;
import com.artezo.dto.response.OrderSummaryResponse;
import com.artezo.dto.response.ShiprocketOrderResponse;
import com.artezo.dto.stats.orders.OrderStats;
import com.artezo.entity.*;
import com.artezo.enum_status.*;
import com.artezo.exceptions.OrderException;
import com.artezo.exceptions.ResourceNotFoundException;
import com.artezo.repository.*;
import com.artezo.service.CouponService;
import com.artezo.service.EmailService;
import com.artezo.service.OrderService;
import com.artezo.service.ShiprocketService;
import com.artezo.util.MagicCheckoutAddressCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);


    @Value("${tax.gst-rate:18.0}")
    private Double gstRate;

    @Value("${tax.calculate-by-state:true}")
    private boolean calculateByState;

    @Value("${shipping.enabled:true}")
    private boolean shippingEnabled;

    @Value("${shipping.default-free-threshold:500}")
    private Double freeShippingThreshold;

    @Value("${shipping.base-charge:50}")
    private Double baseShippingCharge;

    @Value("${shipping.per-kg-charge:10}")
    private Double perKgShippingCharge;

    @Value("${convenience-fee.enabled:false}")
    private boolean convenienceFeeEnabled;

    @Value("${convenience-fee.percentage:0.0}")
    private Double convenienceFeePercent;

    // ADD alongside your existing @Value fields in OrderServiceImpl:
    @Value("${razorpay.key_secret:}")
    private String razorpayKeySecret;

    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;
    private final ShiprocketService shiprocketService;
    private final EmailService emailService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final CheckoutUserRepository checkoutUserRepository;

    private final CustomizationAssetRepository customizationAssetRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final CartItemCustomizationAssetRepository cartItemCustomizationAssetRepository;
    private final OrderItemCustomizationAssetRepository orderItemCustomizationAssetRepository ;

    private final MagicCheckoutAddressCache magicAddressCache;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository, ShiprocketService shiprocketService, EmailService emailService, CouponService couponService, CouponRepository couponRepository, CheckoutUserRepository checkoutUserRepository, CustomizationAssetRepository customizationAssetRepository, CartRepository cartRepository, CartItemRepository cartItemRepository, CartItemCustomizationAssetRepository cartItemCustomizationAssetRepository, OrderItemCustomizationAssetRepository orderItemCustomizationAssetRepository, MagicCheckoutAddressCache magicAddressCache) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.shiprocketService = shiprocketService;
        this.emailService = emailService;
        this.couponService = couponService;
        this.couponRepository = couponRepository;
        this.checkoutUserRepository = checkoutUserRepository;
        this.customizationAssetRepository = customizationAssetRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartItemCustomizationAssetRepository = cartItemCustomizationAssetRepository;
        this.orderItemCustomizationAssetRepository = orderItemCustomizationAssetRepository;
        this.magicAddressCache = magicAddressCache;
    }

    /**
     * AMAZON-STYLE PRICING CALCULATION
     * Calculates tax, shipping, fees like big ecommerce does
     * Returns complete pricing breakdown
     */
    private Map<String, Double> calculatePricingBreakdown(
            OrderItemEntity item,
            String shippingState,
            String shippingPincode) {

        log.info("═══════════════════════════════════════════════════════");
        log.info("  CALCULATING PRICING (E-COM-STYLE)");
        log.info("═══════════════════════════════════════════════════════");

        Map<String, Double> pricing = new HashMap<>();

        // 1. ITEM SUBTOTAL
        double subTotal = item.getItemTotal();
        log.info("► Subtotal (Product × Qty): ₹{}", subTotal);

        // 2. TAX CALCULATION (GST)
        double tax = 0.0;
        if (calculateByState) {
            // State-based GST (all states in India = 18% for most products)
            // In real scenario, lookup state-specific rate from DB
            tax = subTotal * (gstRate / 100.0);
        } else {
            tax = subTotal * (gstRate / 100.0);
        }
        log.info("► Tax ({}% GST): ₹{}", gstRate, String.format("%.2f", tax));

        // 3. SHIPPING CALCULATION
        double shippingCharges = 0.0;
        if (shippingEnabled) {
            // Free shipping above threshold
            if (subTotal >= freeShippingThreshold) {
                shippingCharges = 0.0;
                log.info("► Shipping: ₹0 (FREE — Order > ₹{})", freeShippingThreshold);
            } else {
                // Calculate based on weight
                double weight = item.getWeight() != null ? item.getWeight() : 0.5;
                shippingCharges = baseShippingCharge + (weight * perKgShippingCharge);
                log.info("► Shipping: ₹{} (Base ₹{} + {} kg × ₹{}/kg)",
                        String.format("%.2f", shippingCharges),
                        baseShippingCharge,
                        weight,
                        perKgShippingCharge);
            }
        }

        // 4. CONVENIENCE FEE
        double convenienceFee = 0.0;
        if (convenienceFeeEnabled && convenienceFeePercent > 0) {
            convenienceFee = (subTotal + tax + shippingCharges) * (convenienceFeePercent / 100.0);
            log.info("► Convenience Fee ({}%): ₹{}",
                    convenienceFeePercent,
                    String.format("%.2f", convenienceFee));
        } else {
            log.info("► Convenience Fee: ₹0 (Disabled)");
        }

        // 5. DISCOUNTS (if any from coupon/promo)
        double discountAmount = 0.0;
        double couponDiscount = 0.0;
        // These would come from request if applicable

        // 6. FINAL AMOUNT
        //  double finalAmount = subTotal + tax + shippingCharges + convenienceFee
        //        - discountAmount - couponDiscount;

        // CORRECT FORMULA for Indian E-Commerce
        // Final = Subtotal(selling price) + Tax + Shipping + Convenience - Coupon Only
        double finalAmount = subTotal // already at selling price
                + tax
                + shippingCharges
                + convenienceFee
                - couponDiscount; // ONLY deduct new coupon codes

        log.info("───────────────────────────────────────────────────────");
        log.info("  FINAL CALCULATION (CORRECT FORMULA)");
        log.info("───────────────────────────────────────────────────────");
        log.info("  Subtotal (Selling Price) ₹{}", String.format("%.2f", subTotal));
        log.info("  + Tax (GST)              ₹{}", String.format("%.2f", tax));
        log.info("  + Shipping               ₹{}", String.format("%.2f", shippingCharges));
        log.info("  + Convenience/COD Fee    ₹{}", String.format("%.2f", convenienceFee));
        log.info("  - Coupon Discount        ₹{}", String.format("%.2f", couponDiscount));
        log.info("  = FINAL AMOUNT           ₹{}", String.format("%.2f", finalAmount));
        log.info("═══════════════════════════════════════════════════════");


        log.info("───────────────────────────────────────────────────────");
        log.info("  PRICING BREAKDOWN");
        log.info("───────────────────────────────────────────────────────");
        log.info("  Subtotal              ₹{}", String.format("%.2f", subTotal));
        log.info("  + Tax (GST)           ₹{}", String.format("%.2f", tax));
        log.info("  + Shipping            ₹{}", String.format("%.2f", shippingCharges));
        log.info("  + Convenience Fee     ₹{}", String.format("%.2f", convenienceFee));
        log.info("  - Discounts           ₹{}", String.format("%.2f", discountAmount));
        log.info("───────────────────────────────────────────────────────");
        log.info("  = FINAL AMOUNT        ₹{}", String.format("%.2f", finalAmount));
        log.info("═══════════════════════════════════════════════════════");

        pricing.put("subTotal", subTotal);
        pricing.put("tax", tax);
        pricing.put("shippingCharges", shippingCharges);
        pricing.put("convenienceFee", convenienceFee);
        pricing.put("discountAmount", discountAmount);
        pricing.put("couponDiscount", couponDiscount);
        pricing.put("finalAmount", finalAmount);

        return pricing;
    }

    /**
     * VALIDATE PRICING — ensure calculated amount matches paid amount
     * Critical for fraud prevention
     */
    private void validatePricingAmount(Map<String, Double> calculated, Double paidAmount) {
        Double calculatedFinal = calculated.get("finalAmount");

        // Allow 1 rupee variance (rounding errors)
        double variance = Math.abs(calculatedFinal - paidAmount);

        if (variance > 1.0) {
            log.error("╔════════════════════════════════════════════════════╗");
            log.error("║  ⚠️  AMOUNT MISMATCH — FRAUD ALERT              ║");
            log.error("╚════════════════════════════════════════════════════╝");
            log.error("► Calculated: ₹{}", calculatedFinal);
            log.error("► Paid:       ₹{}", paidAmount);
            log.error("► Variance:   ₹{}", variance);

            throw new OrderException(
                    String.format("Amount mismatch — calculated ₹%.2f but paid ₹%.2f",
                            calculatedFinal, paidAmount),
                    "AMOUNT_MISMATCH"
            );
        }

        log.info("✅ Pricing validation PASSED — variance: ₹{}", variance);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CREATE ORDER — Cart Flow
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order for userId: {}", userId);

        // ── 1. Fetch user ─────────────────────────────────────────────────────
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new OrderException("User not found", "USER_NOT_FOUND"));

        // ── 2. Build order items + validate stock ─────────────────────────────
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

        // ── 3. Calculate final amount ─────────────────────────────────────────
        double couponDiscount  = orZero(request.getCouponDiscount());
        double tax             = 0.0;
        double convenienceFee  = orZero(request.getConvenienceFee());
        double shippingCharges = orZero(request.getShippingCharges());
        double giftwrapCharges = request.isGiftWrap() ? orZero(request.getGiftwrapCharges()) : 0.0;

        double finalAmount = subTotal
                + shippingCharges
                + convenienceFee
                - couponDiscount;

        log.info("✅ Order Pricing Calculated:");
        log.info("  Subtotal:        ₹{}", String.format("%.2f", subTotal));
        log.info("  Tax (GST):        ₹0 (included in selling price)");
        log.info("  Shipping:        ₹{}", String.format("%.2f", shippingCharges));
        log.info("  COD Fee:         ₹{}", String.format("%.2f", convenienceFee));
        log.info("  Coupon Discount: ₹{}", String.format("%.2f", couponDiscount));
        log.info("  ──────────────────────");
        log.info("  Final Amount:    ₹{}", String.format("%.2f", finalAmount));

        // ── 4. Build OrderEntity ──────────────────────────────────────────────
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PLACED);
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
        order.setDiscountAmount(0.0);
        order.setDiscountPercent(0.0);
        order.setCouponCode(request.getCouponCode());
        order.setCouponDiscount(couponDiscount);
        order.setTax(0.0);
        order.setConvenienceFee(convenienceFee);
        order.setShippingCharges(shippingCharges);
        order.setGiftWrap(request.isGiftWrap());
        order.setGiftwrapCharges(giftwrapCharges);
        order.setFinalAmount(finalAmount);
        order.setOrderNotes(request.getOrderNotes());
        order.setOrderFlow(OrderFlow.CART);
        order.setShippingStatus(ShippingStatus.NEW);

        // ── 5. CUSTOMIZATION PATCH: collect slots — DO NOT save yet ──────────
        // Problem: OrderItemEntity has no DB id until orderRepository.save() runs.
        // Saving OrderItemCustomizationAssetEntity before that → TransientPropertyValueException.
        // Solution: collect all slot data in a Map, persist AFTER order is saved.
        //
        // Map structure: OrderItemEntity → List of CartItem slots to transfer
        // ─────────────────────────────────────────────────────────────────────
        Map<OrderItemEntity, List<CartItemCustomizationAssetEntity>> pendingSlots
                = new java.util.LinkedHashMap<>();

        // Get active cart — needed to find CartItems with customization assets
        // Returns null for guest users or if no active cart (non-customized orders)
        CartEntity activeCart = cartRepository
                .findByUser_UserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElse(null);

        for (OrderItemEntity item : orderItems) {
            item.setOrder(order); // link item to order (no DB save yet)

            // ── Skip customization check if no active cart ────────────────────
            if (activeCart == null) continue;

            cartItemRepository
                    .findByCart_IdAndProductIdAndVariantId(
                            activeCart.getId(),
                            productRepository.findByProductStrId(item.getProductStrId())
                                    .map(p -> p.getProductPrimeId()).orElse(null),
                            item.getVariantId()
                    )
                    .ifPresent(cartItem -> {

                        // ── Primary asset: set FK directly on OrderItem ───────────
                        // This is safe — just setting a reference, no DB save yet
                        if (cartItem.getCustomizationAsset() != null) {
                            CustomizationAssetEntity primary = cartItem.getCustomizationAsset();
                            item.setCustomizationAsset(primary);
                            item.setCustomImagePath(primary.getFilePath()); // path snapshot
                            // Mark primary asset ORDERED immediately — safe to save
                            primary.setStatus(CustomizationAssetEntity.AssetStatus.ORDERED);
                            primary.setExpiresAt(null);
                            customizationAssetRepository.save(primary);
                        }

                        // ── Collect all slots for this cart item ──────────────────
                        // DO NOT call orderItemCustomizationAssetRepository.save() here
                        // item.id is still null — FK save will fail with TransientPropertyValueException
                        // We store slots in pendingSlots map and save AFTER orderRepository.save()
                        List<CartItemCustomizationAssetEntity> slots =
                                cartItemCustomizationAssetRepository
                                        .findByCartItem_IdOrderBySlotNumberAsc(cartItem.getId());

                        if (!slots.isEmpty()) {
                            pendingSlots.put(item, slots); // store for post-save processing
                            log.info("[ORDER] Collected {} slots for productStrId={} — will save after order persist",
                                    slots.size(), item.getProductStrId());
                        }
                    });
        }

        order.setOrderItems(orderItems);

        // ── 6. Save order to DB ───────────────────────────────────────────────
        // After this line: order.id, all orderItem.id values are assigned by DB
        // Now safe to save OrderItemCustomizationAssetEntity with valid FK references
        OrderEntity savedOrder = orderRepository.save(order);
        log.info("Order saved to DB: {} — calling Shiprocket...", savedOrder.getOrderStrId());

        // ── 7. NOW persist OrderItemCustomizationAssets ───────────────────────
        // OrderItemEntity ids are now assigned — FK references are valid
        // Loop through pendingSlots collected in step 5
        if (!pendingSlots.isEmpty()) {
            for (Map.Entry<OrderItemEntity, List<CartItemCustomizationAssetEntity>> entry
                    : pendingSlots.entrySet()) {

                OrderItemEntity savedItem   = entry.getKey();
                List<CartItemCustomizationAssetEntity> cartSlots = entry.getValue();

                for (CartItemCustomizationAssetEntity cartSlot : cartSlots) {

                    // ── Build OrderItemCustomizationAssetEntity ────────────────
                    OrderItemCustomizationAssetEntity orderSlot =
                            new OrderItemCustomizationAssetEntity();
                    orderSlot.setOrderItem(savedItem);              // ✅ id exists now
                    orderSlot.setAsset(cartSlot.getAsset());
                    orderSlot.setSlotNumber(cartSlot.getSlotNumber());
                    orderSlot.setFieldName(cartSlot.getFieldName());
                    orderItemCustomizationAssetRepository.save(orderSlot); // ✅ safe now

                    // ── Mark each slot asset as ORDERED ───────────────────────
                    cartSlot.getAsset().setStatus(
                            CustomizationAssetEntity.AssetStatus.ORDERED);
                    cartSlot.getAsset().setExpiresAt(null);
                    customizationAssetRepository.save(cartSlot.getAsset());

                    log.info("[ORDER] Slot {} asset transferred | orderStrId={}, assetUuid={}",
                            cartSlot.getSlotNumber(),
                            savedOrder.getOrderStrId(),
                            cartSlot.getAsset().getAssetUuid());
                }
            }
        }
        // ── END CUSTOMIZATION PATCH ───────────────────────────────────────────

        // ── 8. Call Shiprocket + Send Email on Success ────────────────────────
        try {
            ShiprocketOrderResponse srResponse = shiprocketService.createOrder(savedOrder);

            savedOrder.setShiprocketOrderId(srResponse.getOrderId());
            savedOrder.setShiprocketShipmentId(srResponse.getShipmentId());
            savedOrder.setOrderStatus(OrderStatus.PLACED);

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

            log.info("Order {} placed — SR Order ID: {}",
                    savedOrder.getOrderStrId(), srResponse.getOrderId());

            incrementCouponUsageIfApplied(savedOrder.getCouponCode(), userId);
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
                    mobile,
                    savedOrder.getSubTotal(),           // ← new
                    savedOrder.getDiscountAmount(),      // ← new
                    savedOrder.getCouponDiscount(),      // ← new
                    savedOrder.getTax(),                 // ← new
                    savedOrder.getShippingCharges(),     // ← new
                    savedOrder.getConvenienceFee(),      // ← new
                    savedOrder.getGiftwrapCharges()      // ← new
            );

            log.info("Order confirmation email sent successfully to: {}", customerEmail);

        } catch (Exception emailEx) {
            log.error("Failed to send order confirmation email for order {}: {}",
                    savedOrder.getOrderStrId(), emailEx.getMessage(), emailEx);
            // Do NOT throw — email failure should not rollback the order
        }
    }

    /**
     * Increment coupon usage count after a successful order.
     * Called only once per order, only when a coupon was actually applied.
     * Non-blocking — coupon failure must never rollback the order.
     */
    private void incrementCouponUsageIfApplied(String couponCode, Long userId) {
        if (couponCode == null || couponCode.isBlank()) return;

        try {
            CouponEntity coupon = couponRepository.findByCouponCode(couponCode).orElse(null);
            if (coupon == null) {
                log.warn("[Coupon] Code '{}' not found during usage increment — skipping", couponCode);
                return;
            }

            couponService.incrementUsedCount(coupon.getCouponId(), userId);

            log.info("[Coupon] Usage incremented for code='{}', couponId={}, userId={}",
                    couponCode, coupon.getCouponId(), userId);

        } catch (Exception e) {
            // Non-blocking — order is already placed, just log
            log.error("[Coupon] Failed to increment usage for code='{}' — order still valid: {}",
                    couponCode, e.getMessage());
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
        log.info("╔════════════════════════════════════════════════════╗");
        log.info("║     BUY NOW — ORDER CONFIRMATION STARTED           ║");
        log.info("╚════════════════════════════════════════════════════╝");
        log.info("► User ID        : {}", userId);
        log.info("► SR Order ID    : {}", request.getShiprocketOrderId());
        log.info("► Payment ID     : {}", request.getRazorpayPaymentId());
        log.info("► Product        : {}", request.getProductStrId());
        log.info("► Quantity       : {}", request.getQuantity());
        log.info("► Amount Paid    : ₹{}", request.getAmount());

        // 1. VALIDATE USER
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ User not found: {}", userId);
                    return new OrderException("User not found", "USER_NOT_FOUND");
                });
        log.info("✅ User validated: {}", user.getEmail());

        // 2. FETCH PRODUCT
        ProductEntity product = productRepository.findByProductStrId(request.getProductStrId())
                .orElseThrow(() -> {
                    log.error("❌ Product not found: {}", request.getProductStrId());
                    return new OrderException(
                            "Product not found: " + request.getProductStrId(),
                            "PRODUCT_NOT_FOUND");
                });
        log.info("✅ Product found: {} ({})", product.getProductName(), product.getProductStrId());

        // 3. BUILD ORDER ITEM WITH STOCK VALIDATION
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setProductStrId(request.getProductStrId());
        itemReq.setVariantId(request.getVariantId());
        itemReq.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);

        OrderItemEntity item = buildOrderItem(product, itemReq);
        log.info("✅ Order item built: {} × {}", item.getProductName(), item.getQuantity());

        // 4. CALCULATE PRICING (AMAZON-STYLE)
        Map<String, Double> pricingBreakdown = calculatePricingBreakdown(
                item,
                request.getShippingState(),
                request.getShippingPincode()
        );

        // 5. VALIDATE AMOUNT — Critical security check
        validatePricingAmount(pricingBreakdown, request.getAmount());

        // 6. CREATE ORDER ENTITY
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.PAID);  // ✅ Already paid by SR
        order.setPaymentMethod(PaymentMethod.PREPAID);
        order.setPaymentMode(PaymentMode.RAZORPAY);
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setOrderFlow(OrderFlow.BUY_NOW);
        order.setShippingStatus(ShippingStatus.NEW);

        // Customer details
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setShippingAddress1(request.getShippingAddress1());
        order.setShippingAddress2(request.getShippingAddress2());
        order.setShippingCity(request.getShippingCity());
        order.setShippingState(request.getShippingState());
        order.setShippingPincode(request.getShippingPincode());
        order.setShippingCountry("India");

        // 7. SET ALL PRICING COMPONENTS (NOT JUST SUBTOTAL + FINAL)
        order.setSubTotal(pricingBreakdown.get("subTotal"));
        order.setTax(pricingBreakdown.get("tax"));
        order.setShippingCharges(pricingBreakdown.get("shippingCharges"));
        order.setConvenienceFee(pricingBreakdown.get("convenienceFee"));
        order.setDiscountAmount(pricingBreakdown.get("discountAmount"));
        order.setCouponDiscount(pricingBreakdown.get("couponDiscount"));
        order.setFinalAmount(pricingBreakdown.get("finalAmount"));
        order.setGiftWrap(false);

        // Shiprocket references
        order.setShiprocketOrderId(request.getShiprocketOrderId() != null
                ? Long.parseLong(request.getShiprocketOrderId()) : null);
        order.setShiprocketShipmentId(request.getShiprocketShipmentId() != null
                ? Long.parseLong(request.getShiprocketShipmentId()) : null);

        // Set item relationship
        item.setOrder(order);
        order.setOrderItems(List.of(item));

        // 8. DECREASE STOCK
        decreaseStock(List.of(item));
        log.info("✅ Stock decreased for variant: {}", item.getVariantId());

        // 9. SAVE TO DATABASE
        OrderEntity saved = orderRepository.save(order);
        log.info("╔════════════════════════════════════════════════════╗");
        log.info("║  ✅ ORDER SAVED TO DATABASE SUCCESSFULLY          ║");
        log.info("╚════════════════════════════════════════════════════╝");
        log.info("► Order ID        : {}", saved.getOrderStrId());
        log.info("► Final Amount    : ₹{}", saved.getFinalAmount());
        log.info("► Status          : {}", saved.getOrderStatus());

        // 10. SEND EMAIL (LIKE CART FLOW DOES)
        try {
            sendOrderConfirmationEmail(saved);
            log.info("✅ Order confirmation email sent");
        } catch (Exception emailEx) {
            log.error("⚠️  Email sending failed (non-blocking): {}", emailEx.getMessage());
            // Do NOT throw — email failure should not rollback order
        }

        log.info("═══════════════════════════════════════════════════════");

        return mapToOrderResponse(saved);
    }


    // ────────────────────────────────────────────────────────────────────────
//  CONFIRM MAGIC CHECKOUT ORDER
//  Called after: 1) createPaymentOrder  2) verifyPayment  3) THIS
//  userId is null for guests — checkoutUser entity handles them
// ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse confirmMagicCheckoutOrder(Long userId, MagicCheckoutConfirmRequest request) {
        log.info("╔════════════════════════════════════════════════════╗");
        log.info("║   MAGIC CHECKOUT — ORDER CONFIRMATION STARTED      ║");
        log.info("╚════════════════════════════════════════════════════╝");
        log.info("► userId (logged-in) : {}", userId);
        log.info("► razorpayPaymentId  : {}", request.getRazorpayPaymentId());
        log.info("► razorpayOrderId    : {}", request.getRazorpayOrderId());
        log.info("► phone              : {}", request.getCustomerPhone());
        log.info("► product            : {}", request.getProductStrId());

        // ── 1. SIGNATURE VERIFICATION ────────────────────────────────────────
        // Re-verify here on order-confirm so we don't trust the frontend blindly.
        // Uses same HMAC logic already proven in PaymentServiceImpl.
        boolean signatureValid = verifyMagicSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        if (!signatureValid) {
            log.error("❌ Signature verification FAILED — possible tamper attempt");
            throw new OrderException("Invalid payment signature", "SIGNATURE_INVALID");
        }
        log.info("✅ Signature verified");

        // ── 2. RESOLVE USER — registered OR guest ───────────────────────────
        UserEntity registeredUser = null;
        CheckoutUserEntity checkoutUser = null;

        if (userId != null) {
            registeredUser = userRepository.findById(userId).orElse(null);
            log.info("✅ Logged-in user resolved: {}", userId);
        }

        if (registeredUser == null) {
            // Guest path — upsert CheckoutUserEntity by phone (idempotent)
            String phone = request.getCustomerPhone();
            checkoutUser = checkoutUserRepository.findByPhone(phone)
                    .orElse(new CheckoutUserEntity());

            checkoutUser.setPhone(phone);
            checkoutUser.setName(request.getCustomerName());
            checkoutUser.setEmail(request.getCustomerEmail());
            checkoutUser.setLastAddress1(request.getShippingAddress1());
            checkoutUser.setLastAddress2(request.getShippingAddress2());
            checkoutUser.setLastCity(request.getShippingCity());
            checkoutUser.setLastState(request.getShippingState());
            checkoutUser.setLastPincode(request.getShippingPincode());
            checkoutUser.setLastCountry("India");
            checkoutUser.setSource(CheckoutUserSource.MAGIC_CHECKOUT);

            // Auto-link if a registered user already exists with this phone
            CheckoutUserEntity finalCheckoutUser = checkoutUser;
            userRepository.findByPhone(phone).ifPresent(existing -> {
                finalCheckoutUser.setLinkedUser(existing);
                log.info("► Auto-linked to registered user with same phone: {}", phone);
            });

            checkoutUser = checkoutUserRepository.save(checkoutUser);
            log.info("✅ CheckoutUser upserted — id:{}, phone:{}", checkoutUser.getCheckoutUserId(), phone);
        }

        // ── 3. FETCH PRODUCT ─────────────────────────────────────────────────
        ProductEntity product = productRepository.findByProductStrId(request.getProductStrId())
                .orElseThrow(() -> new OrderException(
                        "Product not found: " + request.getProductStrId(), "PRODUCT_NOT_FOUND"));

        // ── 4. BUILD ORDER ITEM — reuses your existing buildOrderItem helper ──
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setProductStrId(request.getProductStrId());
        itemReq.setVariantId(request.getVariantId());
        itemReq.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);

        OrderItemEntity item = buildOrderItem(product, itemReq);
        log.info("✅ Order item built: {} × {}", item.getProductName(), item.getQuantity());

        // ── 5. CALCULATE + VALIDATE PRICING ─────────────────────────────────
        Map<String, Double> pricing = calculatePricingBreakdown(
                item,
                request.getShippingState(),
                request.getShippingPincode()
        );
        validatePricingAmount(pricing, request.getAmount());

        // ── 6. BUILD ORDER ENTITY ─────────────────────────────────────────────
        OrderEntity order = new OrderEntity();

        // One of these will be non-null
        order.setUser(registeredUser);
        order.setCheckoutUser(checkoutUser);

        order.setOrderStatus(OrderStatus.PLACED);

        // COD → PENDING payment until delivery; else PAID
        boolean isCod = "COD".equalsIgnoreCase(request.getPaymentMethod());
        order.setPaymentStatus(isCod ? PaymentStatus.PENDING : PaymentStatus.PAID);

        order.setPaymentMethod(isCod ? PaymentMethod.COD : PaymentMethod.PREPAID);
        order.setPaymentMode(resolvePaymentMode(request.getPaymentMode(), isCod));
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpayOrderId(request.getRazorpayOrderId());
        order.setOrderFlow(OrderFlow.MAGIC_CHECKOUT);
        order.setShippingStatus(ShippingStatus.NEW);

        // Address snapshot — exact column match to your OrderEntity
        //        order.setCustomerName(request.getCustomerName());
        //        order.setCustomerPhone(request.getCustomerPhone());
        //        order.setCustomerEmail(request.getCustomerEmail());
        //        order.setShippingAddress1(request.getShippingAddress1());
        //        order.setShippingAddress2(request.getShippingAddress2());
        //        order.setShippingCity(request.getShippingCity());
        //        order.setShippingState(request.getShippingState());
        //        order.setShippingPincode(request.getShippingPincode());
        //        order.setShippingCountry("India");

        // REPLACE WITH:
        // Pull from webhook cache first — this is the address user selected in modal
        // Falls back to whatever JS sent if webhook hasn't fired yet (rare)
        MagicCheckoutAddressCache.MagicAddressData cached =
                magicAddressCache.get(request.getRazorpayOrderId()).orElse(null);

        order.setCustomerName(
                cached != null && !cached.name.isBlank()  ? cached.name  : request.getCustomerName());
        order.setCustomerPhone(
                cached != null && !cached.phone.isBlank() ? cached.phone : request.getCustomerPhone());
        order.setCustomerEmail(
                cached != null && !cached.email.isBlank() ? cached.email : request.getCustomerEmail());
        order.setShippingAddress1(
                cached != null ? cached.address1 : request.getShippingAddress1());
        order.setShippingAddress2(
                cached != null ? cached.address2 : request.getShippingAddress2());
        order.setShippingCity(
                cached != null ? cached.city    : request.getShippingCity());
        order.setShippingState(
                cached != null ? cached.state   : request.getShippingState());
        order.setShippingPincode(
                cached != null ? cached.pincode : request.getShippingPincode());
        order.setShippingCountry("India");

        // Clean up cache after consuming
        magicAddressCache.remove(request.getRazorpayOrderId());

        // Pricing — from your calculatePricingBreakdown (same as confirmBuyNowOrder)
        order.setSubTotal(pricing.get("subTotal"));
        order.setTax(pricing.get("tax"));
        order.setShippingCharges(
                request.getShippingCharges() != null ? request.getShippingCharges() : pricing.get("shippingCharges")
        );
        order.setConvenienceFee(
                request.getConvenienceFee() != null ? request.getConvenienceFee() : pricing.get("convenienceFee")
        );
        order.setDiscountAmount(0.0);
        order.setDiscountPercent(0.0);
        order.setCouponCode(request.getCouponCode());
        order.setCouponDiscount(
                request.getCouponDiscount() != null ? request.getCouponDiscount() : pricing.get("couponDiscount")
        );
        order.setGiftWrap(false);
        order.setFinalAmount(pricing.get("finalAmount"));

        item.setOrder(order);
        order.setOrderItems(List.of(item));

        // ── 7. SAVE TO DB ────────────────────────────────────────────────────
        OrderEntity saved = orderRepository.save(order);
        log.info("✅ Order saved — {}", saved.getOrderStrId());

        // ── 8. SHIPROCKET PUSH — same as createOrder() ───────────────────────
        try {
            ShiprocketOrderResponse srResponse = shiprocketService.createOrder(saved);
            saved.setShiprocketOrderId(srResponse.getOrderId());
            saved.setShiprocketShipmentId(srResponse.getShipmentId());
            if (srResponse.getAwbCode() != null && !srResponse.getAwbCode().isEmpty()) {
                saved.setAwbNumber(srResponse.getAwbCode());
            }
            if (srResponse.getCourierName() != null) {
                saved.setCourierName(srResponse.getCourierName());
            }
            saved.setOrderStatus(OrderStatus.PLACED);
            decreaseStock(saved.getOrderItems());
            orderRepository.save(saved);
            log.info("✅ Shiprocket order created — SR ID: {}", srResponse.getOrderId());
        } catch (Exception e) {
            log.error("Shiprocket push failed for {} — saved as PENDING: {}",
                    saved.getOrderStrId(), e.getMessage());
            saved.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(saved);
        }

        // ── 9. COUPON USAGE (only for logged-in users) ────────────────────────
        if (userId != null) {
            incrementCouponUsageIfApplied(saved.getCouponCode(), userId);
        }

        // ── 10. EMAIL ─────────────────────────────────────────────────────────
        try {
            sendOrderConfirmationEmail(saved);
        } catch (Exception e) {
            log.error("Email failed (non-blocking): {}", e.getMessage());
        }

        log.info("╔════════════════════════════════════════════════════╗");
        log.info("║  ✅ MAGIC CHECKOUT ORDER COMPLETE — {}  ║", saved.getOrderStrId());
        log.info("╚════════════════════════════════════════════════════╝");

        return mapToOrderResponse(saved);
    }

    // ── Signature verify — mirrors PaymentServiceImpl logic exactly ───────────
// Avoids cross-service dependency. Same HMAC-SHA256 pattern.
    private boolean verifyMagicSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString().equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // ── Resolve PaymentMode safely ─────────────────────────────────────────────
    private PaymentMode resolvePaymentMode(String mode, boolean isCod) {
        if (isCod) return PaymentMode.COD;
        if (mode == null || mode.isBlank()) return PaymentMode.RAZORPAY;
        try {
            return PaymentMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PaymentMode.RAZORPAY;
        }
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
    public Page<OrderSummaryResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository
                .findAllByOrderByOrderDateDesc(pageable)
                .map(this::mapToOrderSummaryResponse);
    }



    private OrderSummaryResponse mapToOrderSummaryResponse(OrderEntity order) {
        OrderSummaryResponse res = new OrderSummaryResponse();

        res.setOrderId(order.getOrderId());
        res.setOrderStrId(order.getOrderStrId());
        res.setOrderDate(order.getOrderDate());
        res.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        res.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        res.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        res.setPaymentMode(order.getPaymentMode() != null ? order.getPaymentMode().name() : null);
        res.setCustomerName(order.getCustomerName());
        res.setCustomerPhone(order.getCustomerPhone());
        res.setSubTotal(order.getSubTotal());
        res.setDiscountAmount(order.getDiscountAmount());
        res.setDiscountPercent(order.getDiscountPercent());
        res.setTax(order.getTax());
        res.setFinalAmount(order.getFinalAmount());
        res.setCourierName(order.getCourierName());
        res.setOrderNotes(order.getOrderNotes());
        res.setReturnRequested(order.isReturnRequested());
        res.setExchangeRequested(order.isExchangeRequested());

        if (order.getOrderItems() != null) {
            List<OrderSummaryResponse.OrderItemSummary> itemSummaries = order.getOrderItems()
                    .stream().map(i -> {
                        OrderSummaryResponse.OrderItemSummary ir = new OrderSummaryResponse.OrderItemSummary();
                        ir.setProductName(i.getProductName());
                        ir.setColor(i.getColor());
                        ir.setQuantity(i.getQuantity());
                        ir.setSellingPrice(i.getSellingPrice());
                        ir.setItemTotal(i.getItemTotal());

                        // Product image URL
                        if (i.getProductStrId() != null) {
                            productRepository.findByProductStrId(i.getProductStrId())
                                    .ifPresent(product -> {
                                        Long primeId = product.getProductPrimeId();
                                        String variantId = i.getVariantId();

                                        if (variantId != null && !variantId.isBlank()) {
                                            boolean variantHasImage = product.getVariants().stream()
                                                    .filter(v -> variantId.equals(v.getVariantId()))
                                                    .findFirst()
                                                    .map(v -> v.getMainImageData() != null && v.getMainImageData().length > 0)
                                                    .orElse(false);

                                            ir.setProductImageUrl(variantHasImage
                                                    ? "/api/products/" + primeId + "/variant/" + variantId + "/main"
                                                    : "/api/products/" + primeId + "/main");
                                        } else {
                                            ir.setProductImageUrl("/api/products/" + primeId + "/main");
                                        }
                                    });
                        }

                        // ── CUSTOMIZATION PATCH: admin orders list ────────────────────────────────
                        if (i.getCustomizationAsset() != null) {
                            ir.setProductImageUrl("/api/v1/customize/image/" +
                                    i.getCustomizationAsset().getAssetUuid());
                        }
// ─────────────────────────────────────────────────────────────────────────
                        return ir;
                    }).collect(Collectors.toList());

            res.setOrderItems(itemSummaries);
        }

        return res;
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

        res.setOrderId(order.getOrderId());
        res.setOrderStrId(order.getOrderStrId());
        res.setOrderDate(order.getOrderDate());
        res.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        res.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        res.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        res.setPaymentMode(order.getPaymentMode() != null ? order.getPaymentMode().name() : null);
        res.setOrderFlow(order.getOrderFlow() != null ? order.getOrderFlow().name() : null);

        if (order.getCheckoutUser() != null) {
            res.setCheckoutUserId(order.getCheckoutUser().getCheckoutUserId());
            res.setCheckoutUserPhone(order.getCheckoutUser().getPhone());
        }

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
                                        ir.setProductPrimeId(primeId);   // ← ADD THIS LINE — was never called

                                        ir.setIsCustomizable(product.getIsCustomizable());

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
                        // ── CUSTOMIZATION PATCH: override productImageUrl for customized items ────
                        // If this order item has a custom image, show THAT instead of product image.
                        // Admin panel + user order detail will automatically show the uploaded image.
                        // Normal items: customizationAsset is null → this block skipped entirely.
                        if (i.getCustomizationAsset() != null) {
                            String assetUuid = i.getCustomizationAsset().getAssetUuid();
                            ir.setProductImageUrl("/api/v1/customize/image/" + assetUuid);
                            ir.setCustomImagePath(i.getCustomImagePath()); // ← snapshot path for audit
                            log.debug("[ORDER-RESPONSE] Custom image set for orderItem | assetUuid={}", assetUuid);
                        }
                        // ── END CUSTOMIZATION PATCH ───────────────────────────────────────────────


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


    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderId(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)  // or findByOrderStrId depending on your param
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderResponse response = mapToOrderResponse(order);

        // Fetch user stats (one extra query - very lightweight)
        OrderStats stats = orderRepository.getOrderStatsByUserId(order.getUser().getUserId());

        response.setTotalOrdersCount(BigDecimal.valueOf(stats.getTotalOrdersCount()));
        response.setTotalSpent(stats.getTotalSpent());

        return response;
    }

    @Override
    @Transactional
    public OrderResponse patchOrder(Long orderId, Map<String, Object> fields) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        fields.forEach((key, value) -> {
            switch (key) {
                case "orderStatus"      -> order.setOrderStatus(OrderStatus.valueOf((String) value));
                case "paymentStatus"    -> order.setPaymentStatus(PaymentStatus.valueOf((String) value));
                case "paymentMethod"    -> order.setPaymentMethod(PaymentMethod.valueOf((String) value));
                case "paymentMode"      -> order.setPaymentMode(PaymentMode.valueOf((String) value));
                case "orderFlow"        -> order.setOrderFlow(OrderFlow.valueOf((String) value));
                case "customerName"     -> order.setCustomerName((String) value);
                case "customerPhone"    -> order.setCustomerPhone((String) value);
                case "customerEmail"    -> order.setCustomerEmail((String) value);
                case "shippingAddress1" -> order.setShippingAddress1((String) value);
                case "shippingAddress2" -> order.setShippingAddress2((String) value);
                case "shippingCity"     -> order.setShippingCity((String) value);
                case "shippingState"    -> order.setShippingState((String) value);
                case "shippingPincode"  -> order.setShippingPincode((String) value);
                case "subTotal"         -> order.setSubTotal(((Number) value).doubleValue());
                case "discountAmount"   -> order.setDiscountAmount(((Number) value).doubleValue());
                case "discountPercent"  -> order.setDiscountPercent(((Number) value).doubleValue());
                case "couponCode"       -> order.setCouponCode((String) value);
                case "couponDiscount"   -> order.setCouponDiscount(((Number) value).doubleValue());
                case "tax"              -> order.setTax(((Number) value).doubleValue());
                case "convenienceFee"   -> order.setConvenienceFee(((Number) value).doubleValue());
                case "shippingCharges"  -> order.setShippingCharges(((Number) value).doubleValue());
                case "giftwrapCharges"  -> order.setGiftwrapCharges(((Number) value).doubleValue());
                case "finalAmount"      -> order.setFinalAmount(((Number) value).doubleValue());
                case "giftWrap"         -> order.setGiftWrap((Boolean) value);
                case "orderNotes"       -> order.setOrderNotes((String) value);
                case "awbNumber"        -> order.setAwbNumber((String) value);
                case "courierName"      -> order.setCourierName((String) value);
                case "shippingStatus"   -> order.setShippingStatus(ShippingStatus.valueOf((String) value));
                case "returnRequested"  -> order.setReturnRequested((Boolean) value);
                case "exchangeRequested"-> order.setExchangeRequested((Boolean) value);
                case "returnReason"     -> order.setReturnReason((String) value);
                case "exchangeReason"   -> order.setExchangeReason((String) value);
                case "cancelledAt"      -> order.setCancelledAt(LocalDateTime.parse((String) value));
                case "deliveredAt"      -> order.setDeliveredAt(LocalDateTime.parse((String) value));
                default                 -> throw new IllegalArgumentException("Unknown or non-patchable field: " + key);
            }
        });

        return mapToOrderResponse(orderRepository.save(order));
    }


    // ────────────────────────────────────────────────────────────────────────
    //  CANCEL EXCHANGE REQUEST
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse cancelExchangeRequest(Long userId, String orderStrId) {
        OrderEntity order = getOrderAndValidateOwner(userId, orderStrId);

        if (order.getOrderStatus() != OrderStatus.EXCHANGE_REQUESTED) {
            throw new OrderException(
                    "Only exchange requested orders can be cancelled. Current status: " + order.getOrderStatus(),
                    "INVALID_STATUS_FOR_CANCEL_EXCHANGE");
        }

        // Reset exchange fields
        order.setExchangeRequested(false);
        order.setExchangeReason(null);
        order.setExchangeRequestedAt(null);
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.getOrderItems().forEach(i -> i.setItemStatus(ItemStatus.ACTIVE));

        // Optional: Call Shiprocket to cancel exchange if needed
        if (order.getExchangeShiprocketOrderId() != null) {
            try {
                shiprocketService.cancelOrder(order.getExchangeShiprocketOrderId());
            } catch (Exception e) {
                log.warn("SR cancel exchange failed for {}: {}", orderStrId, e.getMessage());
            }
        }

        OrderEntity saved = orderRepository.save(order);
        log.info("Exchange request cancelled for order {}", orderStrId);
        return mapToOrderResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CANCEL RETURN REQUEST
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse cancelReturnRequest(Long userId, String orderStrId) {
        OrderEntity order = getOrderAndValidateOwner(userId, orderStrId);

        if (order.getOrderStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new OrderException(
                    "Only return requested orders can be cancelled. Current status: " + order.getOrderStatus(),
                    "INVALID_STATUS_FOR_CANCEL_RETURN");
        }

        // Reset return fields
        order.setReturnRequested(false);
        order.setReturnReason(null);
        order.setReturnRequestedAt(null);
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.getOrderItems().forEach(i -> i.setItemStatus(ItemStatus.ACTIVE));

        // Optional: Call Shiprocket to cancel return if needed
        if (order.getReturnShiprocketOrderId() != null) {
            try {
                shiprocketService.cancelOrder(order.getReturnShiprocketOrderId());
            } catch (Exception e) {
                log.warn("SR cancel return failed for {}: {}", orderStrId, e.getMessage());
            }
        }

        OrderEntity saved = orderRepository.save(order);
        log.info("Return request cancelled for order {}", orderStrId);
        return mapToOrderResponse(saved);
    }

}
