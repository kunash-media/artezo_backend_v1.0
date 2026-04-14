package com.artezo.entity;

import com.artezo.enum_status.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(unique = true, nullable = true)
    private String orderStrId;          // ORD-20250314-0001

    @CreationTimestamp
    private LocalDateTime orderDate;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── USER REFERENCE ────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;            // FK → your existing UserEntity


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    // ── ORDER STATUS ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")  //support new enum in mysql column
    private OrderStatus orderStatus;

    // ── PAYMENT ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // PENDING, PAID, FAILED, REFUNDED

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod; // PREPAID, COD

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;     // RAZORPAY, UPI, NETBANKING, CARD, COD

    private String razorpayPaymentId;    // rzp_live_XXXXXXXXXX
    private String razorpayOrderId;      // order_XXXXXXXXXX (Razorpay order id)

    // ── CUSTOMER / SHIPPING ADDRESS ───────────────────────────────────────────
    // Billing is always same as shipping per your requirement —
    // Shiprocket's shipping_is_billing flag will always be set to true.

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private String shippingAddress1;
    private String shippingAddress2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;

    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'India'")
    private String shippingCountry = "India";

    // ── ORDER ITEMS ───────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    // ── PRICING SUMMARY ───────────────────────────────────────────────────────

    private Double subTotal;            // sum of (sellingPrice × qty) before discounts

    private Double discountAmount;      // flat discount value applied
    private Double discountPercent;     // discount % applied (stored for display)

    private String couponCode;          // coupon code string e.g. "SAVE100"
    private Double couponDiscount;      // amount saved via coupon

    private Double tax;                 // GST / tax amount
    private Double convenienceFee;      // platform convenience fee if any
    private Double shippingCharges;     // shipping charge (0 if free shipping)
    private Double giftwrapCharges;     // gift wrap add-on charge

    private Double finalAmount;         // what customer actually pays
    // = subTotal - discountAmount - couponDiscount
    //   + tax + convenienceFee + shippingCharges
    //   + giftwrapCharges

    // ── GIFT WRAP & NOTES ─────────────────────────────────────────────────────

    @Column(columnDefinition = "boolean default false")
    private boolean giftWrap = false;

    @Column(columnDefinition = "TEXT")
    private String orderNotes;          // special instructions from customer

    // ── SHIPROCKET SYNC ───────────────────────────────────────────────────────

    private Long shiprocketOrderId;     // SR numeric order id returned on create
    private Long shiprocketShipmentId;  // SR shipment id returned on create
    private String awbNumber;           // courier tracking number assigned by SR
    private String courierName;         // e.g. Delhivery, BlueDart, Ekart

    @Enumerated(EnumType.STRING)
    private ShippingStatus shippingStatus; // NEW, PICKUP_SCHEDULED, IN_TRANSIT,
    // OUT_FOR_DELIVERY, DELIVERED, FAILED_DELIVERY

    private String pickupLocation;      // warehouse name configured in SR panel

    // ── RETURN / EXCHANGE / CANCEL ────────────────────────────────────────────

    @Column(columnDefinition = "boolean default false")
    private boolean returnRequested = false;

    @Column(columnDefinition = "boolean default false")
    private boolean exchangeRequested = false;

    private String returnReason;
    private String exchangeReason;

    private Long returnShiprocketOrderId;   // SR return order id after return created
    private Long exchangeShiprocketOrderId; // SR new forward order id after exchange

    private LocalDateTime cancelledAt;
    private LocalDateTime returnRequestedAt;
    private LocalDateTime exchangeRequestedAt;
    private LocalDateTime deliveredAt;

    // ── FLOW TYPE ─────────────────────────────────────────────────────────────
    // Tells you whether this order came from Buy Now (SR Checkout)
    // or Add to Cart (your checkout page).

    @Enumerated(EnumType.STRING)
    private OrderFlow orderFlow;        // BUY_NOW, CART


    // OrderEntity.java — add this method
    @PostPersist
    private void generateOrderStrId() {
        this.orderStrId = "ORD-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + this.orderId;
    }


    public OrderEntity() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderStrId() {
        return orderStrId;
    }

    public void setOrderStrId(String orderStrId) {
        this.orderStrId = orderStrId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getShippingAddress1() {
        return shippingAddress1;
    }

    public void setShippingAddress1(String shippingAddress1) {
        this.shippingAddress1 = shippingAddress1;
    }

    public String getShippingAddress2() {
        return shippingAddress2;
    }

    public void setShippingAddress2(String shippingAddress2) {
        this.shippingAddress2 = shippingAddress2;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingState() {
        return shippingState;
    }

    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }

    public String getShippingPincode() {
        return shippingPincode;
    }

    public void setShippingPincode(String shippingPincode) {
        this.shippingPincode = shippingPincode;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public List<OrderItemEntity> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemEntity> orderItems) {
        this.orderItems = orderItems;
    }

    public Double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(Double subTotal) {
        this.subTotal = subTotal;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public Double getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(Double couponDiscount) {
        this.couponDiscount = couponDiscount;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getConvenienceFee() {
        return convenienceFee;
    }

    public void setConvenienceFee(Double convenienceFee) {
        this.convenienceFee = convenienceFee;
    }

    public Double getShippingCharges() {
        return shippingCharges;
    }

    public void setShippingCharges(Double shippingCharges) {
        this.shippingCharges = shippingCharges;
    }

    public Double getGiftwrapCharges() {
        return giftwrapCharges;
    }

    public void setGiftwrapCharges(Double giftwrapCharges) {
        this.giftwrapCharges = giftwrapCharges;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public boolean isGiftWrap() {
        return giftWrap;
    }

    public void setGiftWrap(boolean giftWrap) {
        this.giftWrap = giftWrap;
    }

    public String getOrderNotes() {
        return orderNotes;
    }

    public void setOrderNotes(String orderNotes) {
        this.orderNotes = orderNotes;
    }

    public Long getShiprocketOrderId() {
        return shiprocketOrderId;
    }

    public void setShiprocketOrderId(Long shiprocketOrderId) {
        this.shiprocketOrderId = shiprocketOrderId;
    }

    public Long getShiprocketShipmentId() {
        return shiprocketShipmentId;
    }

    public void setShiprocketShipmentId(Long shiprocketShipmentId) {
        this.shiprocketShipmentId = shiprocketShipmentId;
    }

    public String getAwbNumber() {
        return awbNumber;
    }

    public void setAwbNumber(String awbNumber) {
        this.awbNumber = awbNumber;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public ShippingStatus getShippingStatus() {
        return shippingStatus;
    }

    public void setShippingStatus(ShippingStatus shippingStatus) {
        this.shippingStatus = shippingStatus;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public boolean isReturnRequested() {
        return returnRequested;
    }

    public void setReturnRequested(boolean returnRequested) {
        this.returnRequested = returnRequested;
    }

    public boolean isExchangeRequested() {
        return exchangeRequested;
    }

    public void setExchangeRequested(boolean exchangeRequested) {
        this.exchangeRequested = exchangeRequested;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public String getExchangeReason() {
        return exchangeReason;
    }

    public void setExchangeReason(String exchangeReason) {
        this.exchangeReason = exchangeReason;
    }

    public Long getReturnShiprocketOrderId() {
        return returnShiprocketOrderId;
    }

    public void setReturnShiprocketOrderId(Long returnShiprocketOrderId) {
        this.returnShiprocketOrderId = returnShiprocketOrderId;
    }

    public Long getExchangeShiprocketOrderId() {
        return exchangeShiprocketOrderId;
    }

    public void setExchangeShiprocketOrderId(Long exchangeShiprocketOrderId) {
        this.exchangeShiprocketOrderId = exchangeShiprocketOrderId;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getReturnRequestedAt() {
        return returnRequestedAt;
    }

    public void setReturnRequestedAt(LocalDateTime returnRequestedAt) {
        this.returnRequestedAt = returnRequestedAt;
    }

    public LocalDateTime getExchangeRequestedAt() {
        return exchangeRequestedAt;
    }

    public void setExchangeRequestedAt(LocalDateTime exchangeRequestedAt) {
        this.exchangeRequestedAt = exchangeRequestedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public OrderFlow getOrderFlow() {
        return orderFlow;
    }

    public void setOrderFlow(OrderFlow orderFlow) {
        this.orderFlow = orderFlow;
    }


    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }
}
