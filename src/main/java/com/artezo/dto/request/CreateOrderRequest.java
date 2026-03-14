package com.artezo.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateOrderRequest {

    // ── Customer / Address ────────────────────────────────────────────────
    @NotNull(message = "Customer name is required")
    private String customerName;

    @NotNull(message = "Customer phone is required")
    private String customerPhone;

    private String customerEmail;

    @NotNull(message = "Shipping address is required")
    private String shippingAddress1;

    private String shippingAddress2;

    @NotNull(message = "City is required")
    private String shippingCity;

    @NotNull(message = "State is required")
    private String shippingState;

    @NotNull(message = "Pincode is required")
    private String shippingPincode;

    // ── Payment ───────────────────────────────────────────────────────────
    @NotNull(message = "Payment method is required")
    private String paymentMethod;       // "PREPAID" or "COD"

    private String paymentMode;         // "RAZORPAY", "UPI", "NETBANKING", "CARD", "COD"
    private String razorpayPaymentId;
    private String razorpayOrderId;

    // ── Items ─────────────────────────────────────────────────────────────
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemRequest> items;

    // ── Pricing ───────────────────────────────────────────────────────────
    private String couponCode;
    private Double couponDiscount;
    private Double discountAmount;
    private Double discountPercent;
    private Double shippingCharges;
    private Double convenienceFee;
    private Double tax;

    // ── Extras ───────────────────────────────────────────────────────────
    private boolean giftWrap = false;
    private Double giftwrapCharges;
    private String orderNotes;

    // getters and setters ...


    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
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

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
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

    public Double getShippingCharges() {
        return shippingCharges;
    }

    public void setShippingCharges(Double shippingCharges) {
        this.shippingCharges = shippingCharges;
    }

    public Double getConvenienceFee() {
        return convenienceFee;
    }

    public void setConvenienceFee(Double convenienceFee) {
        this.convenienceFee = convenienceFee;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public boolean isGiftWrap() {
        return giftWrap;
    }

    public void setGiftWrap(boolean giftWrap) {
        this.giftWrap = giftWrap;
    }

    public Double getGiftwrapCharges() {
        return giftwrapCharges;
    }

    public void setGiftwrapCharges(Double giftwrapCharges) {
        this.giftwrapCharges = giftwrapCharges;
    }

    public String getOrderNotes() {
        return orderNotes;
    }

    public void setOrderNotes(String orderNotes) {
        this.orderNotes = orderNotes;
    }

    // ── Inner: Order Item ─────────────────────────────────────────────────
    public static class OrderItemRequest {

        @NotNull
        private String productStrId;    // PRD00001

        private String variantId;       // VAR-GOLD — null if no variant

        @NotNull
        private Integer quantity;

        // getters and setters ...


        public String getProductStrId() {
            return productStrId;
        }

        public void setProductStrId(String productStrId) {
            this.productStrId = productStrId;
        }

        public String getVariantId() {
            return variantId;
        }

        public void setVariantId(String variantId) {
            this.variantId = variantId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}