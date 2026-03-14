package com.artezo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ShiprocketReturnRequest {

    @JsonProperty("order_id")
    private String orderId;                 // your original orderStrId

    @JsonProperty("order_date")
    private String orderDate;

    @JsonProperty("channel_id")
    private Integer channelId = 0;

    // ── Pickup = Customer Address (returning FROM customer) ───────────────
    @JsonProperty("pickup_customer_name")
    private String pickupCustomerName;

    @JsonProperty("pickup_phone")
    private String pickupPhone;

    @JsonProperty("pickup_address")
    private String pickupAddress;

    @JsonProperty("pickup_city")
    private String pickupCity;

    @JsonProperty("pickup_state")
    private String pickupState;

    @JsonProperty("pickup_country")
    private String pickupCountry = "India";

    @JsonProperty("pickup_pincode")
    private String pickupPincode;

    // ── Shipping = Your Warehouse Address (returning TO you) ──────────────
    @JsonProperty("shipping_customer_name")
    private String shippingCustomerName;    // your store name

    @JsonProperty("shipping_phone")
    private String shippingPhone;           // your store phone

    @JsonProperty("shipping_address")
    private String shippingAddress;         // your warehouse address

    @JsonProperty("shipping_city")
    private String shippingCity;

    @JsonProperty("shipping_state")
    private String shippingState;

    @JsonProperty("shipping_country")
    private String shippingCountry = "India";

    @JsonProperty("shipping_pincode")
    private String shippingPincode;

    // ── Payment & Items ───────────────────────────────────────────────────
    @JsonProperty("payment_method")
    private String paymentMethod = "Prepaid";

    @JsonProperty("sub_total")
    private Double subTotal;

    @JsonProperty("length")
    private Double length;

    @JsonProperty("breadth")
    private Double breadth;

    @JsonProperty("height")
    private Double height;

    @JsonProperty("weight")
    private Double weight;

    @JsonProperty("order_items")
    private List<ShiprocketOrderRequest.ShiprocketOrderItem> orderItems;

    // getters and setters ...


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getPickupCustomerName() {
        return pickupCustomerName;
    }

    public void setPickupCustomerName(String pickupCustomerName) {
        this.pickupCustomerName = pickupCustomerName;
    }

    public String getPickupPhone() {
        return pickupPhone;
    }

    public void setPickupPhone(String pickupPhone) {
        this.pickupPhone = pickupPhone;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getPickupCity() {
        return pickupCity;
    }

    public void setPickupCity(String pickupCity) {
        this.pickupCity = pickupCity;
    }

    public String getPickupState() {
        return pickupState;
    }

    public void setPickupState(String pickupState) {
        this.pickupState = pickupState;
    }

    public String getPickupCountry() {
        return pickupCountry;
    }

    public void setPickupCountry(String pickupCountry) {
        this.pickupCountry = pickupCountry;
    }

    public String getPickupPincode() {
        return pickupPincode;
    }

    public void setPickupPincode(String pickupPincode) {
        this.pickupPincode = pickupPincode;
    }

    public String getShippingCustomerName() {
        return shippingCustomerName;
    }

    public void setShippingCustomerName(String shippingCustomerName) {
        this.shippingCustomerName = shippingCustomerName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
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

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
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

    public Double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(Double subTotal) {
        this.subTotal = subTotal;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getBreadth() {
        return breadth;
    }

    public void setBreadth(Double breadth) {
        this.breadth = breadth;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public List<ShiprocketOrderRequest.ShiprocketOrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<ShiprocketOrderRequest.ShiprocketOrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}