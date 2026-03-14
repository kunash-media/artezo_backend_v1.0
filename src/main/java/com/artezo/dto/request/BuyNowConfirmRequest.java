package com.artezo.dto.request;

public class BuyNowConfirmRequest {

    private String shiprocketOrderId;   // SR order id from checkout callback
    private String shiprocketShipmentId;
    private String razorpayPaymentId;   // payment id from SR checkout callback
    private Double amount;              // final amount paid

    private String productStrId;        // which product was purchased
    private String variantId;           // which variant — null if no variant
    private Integer quantity;

    // Customer details filled by SR Checkout widget
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress1;
    private String shippingAddress2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;

    // getters and setters ...


    public String getShiprocketOrderId() {
        return shiprocketOrderId;
    }

    public void setShiprocketOrderId(String shiprocketOrderId) {
        this.shiprocketOrderId = shiprocketOrderId;
    }

    public String getShiprocketShipmentId() {
        return shiprocketShipmentId;
    }

    public void setShiprocketShipmentId(String shiprocketShipmentId) {
        this.shiprocketShipmentId = shiprocketShipmentId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

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
}