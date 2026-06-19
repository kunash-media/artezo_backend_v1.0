package com.artezo.dto.request;

public class MagicCheckoutConfirmRequest {

    // ── Razorpay Payment Proof ─────────────────────────────────────────────
    private String razorpayPaymentId;       // from handler: response.razorpay_payment_id
    private String razorpayOrderId;         // from handler: response.razorpay_order_id
    private String razorpaySignature;       // from handler: response.razorpay_signature

    // ── Product ───────────────────────────────────────────────────────────
    private String productStrId;            // PRD0001
    private String variantId;               // VAR-GOLD — null if no variant
    private Integer quantity;               // default 1

    // ── Payment ───────────────────────────────────────────────────────────
    // paymentMethod → PREPAID or COD
    // paymentMode   → RAZORPAY, UPI, CARD, NETBANKING, COD
    private String paymentMethod;
    private String paymentMode;

    // ── Address — from Razorpay modal (what user selected/entered) ─────────
    // These field names intentionally match OrderEntity + CreateOrderRequest
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress1;
    private String shippingAddress2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;

    // ── Coupon (if applied via Magic Checkout coupon UI) ───────────────────
    private String couponCode;
    private Double couponDiscount;

    // ── Amount — in RUPEES — for server-side pricing validation ───────────
    // Send: order.amount / 100 (convert from paise)
    private Double amount;

    // ── Shipping charges from Magic Checkout shipping endpoint ────────────
    private Double shippingCharges;

    // ── Convenience fee (COD fee if paymentMethod = COD) ─────────────────
    private Double convenienceFee;

    // Getters / Setters

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }

    public String getProductStrId() { return productStrId; }
    public void setProductStrId(String productStrId) { this.productStrId = productStrId; }

    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getShippingAddress1() { return shippingAddress1; }
    public void setShippingAddress1(String shippingAddress1) { this.shippingAddress1 = shippingAddress1; }

    public String getShippingAddress2() { return shippingAddress2; }
    public void setShippingAddress2(String shippingAddress2) { this.shippingAddress2 = shippingAddress2; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingState() { return shippingState; }
    public void setShippingState(String shippingState) { this.shippingState = shippingState; }

    public String getShippingPincode() { return shippingPincode; }
    public void setShippingPincode(String shippingPincode) { this.shippingPincode = shippingPincode; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public Double getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(Double couponDiscount) { this.couponDiscount = couponDiscount; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getShippingCharges() { return shippingCharges; }
    public void setShippingCharges(Double shippingCharges) { this.shippingCharges = shippingCharges; }

    public Double getConvenienceFee() { return convenienceFee; }
    public void setConvenienceFee(Double convenienceFee) { this.convenienceFee = convenienceFee; }
}