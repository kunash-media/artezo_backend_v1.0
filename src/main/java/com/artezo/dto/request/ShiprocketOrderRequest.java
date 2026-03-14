package com.artezo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ShiprocketOrderRequest {

    // ── Order Identity ────────────────────────────────────────────────────
    @JsonProperty("order_id")
    private String orderId;                 // your orderStrId e.g. ORD-20250314-0001

    @JsonProperty("order_date")
    private String orderDate;               // "2025-03-14 10:00"

    @JsonProperty("pickup_location")
    private String pickupLocation;          // warehouse name in SR panel e.g. "Primary"

    @JsonProperty("comment")
    private String comment;                 // orderNotes

    @JsonProperty("channel_id")
    private String channelId = "";          // leave blank for custom integration

    // ── Billing / Shipping (always same per your requirement) ─────────────
    @JsonProperty("billing_customer_name")
    private String billingCustomerName;

    @JsonProperty("billing_last_name")
    private String billingLastName = "";

    @JsonProperty("billing_address")
    private String billingAddress;

    @JsonProperty("billing_address_2")
    private String billingAddress2 = "";

    @JsonProperty("billing_city")
    private String billingCity;

    @JsonProperty("billing_pincode")
    private String billingPincode;

    @JsonProperty("billing_state")
    private String billingState;

    @JsonProperty("billing_country")
    private String billingCountry = "India";

    @JsonProperty("billing_email")
    private String billingEmail;

    @JsonProperty("billing_phone")
    private String billingPhone;

    @JsonProperty("shipping_is_billing")
    private boolean shippingIsBilling = true;   // always true per your requirement

    // ── Order Items ───────────────────────────────────────────────────────
    @JsonProperty("order_items")
    private List<ShiprocketOrderItem> orderItems;

    // ── Payment ───────────────────────────────────────────────────────────
    @JsonProperty("payment_method")
    private String paymentMethod;           // "Prepaid" or "COD"

    // ── Pricing ───────────────────────────────────────────────────────────
    @JsonProperty("sub_total")
    private Double subTotal;

    @JsonProperty("shipping_charges")
    private Double shippingCharges = 0.0;

    @JsonProperty("giftwrap_charges")
    private Double giftwrapCharges = 0.0;

    @JsonProperty("transaction_charges")
    private Double transactionCharges = 0.0;

    @JsonProperty("total_discount")
    private Double totalDiscount = 0.0;

    // ── Shipment Dimensions ───────────────────────────────────────────────
    // For multi-item orders: use dimensions of the heaviest/largest item
    // OR sum weights and use max dimensions — SR needs one set per shipment
    @JsonProperty("length")
    private Double length;

    @JsonProperty("breadth")
    private Double breadth;

    @JsonProperty("height")
    private Double height;

    @JsonProperty("weight")
    private Double weight;

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

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getBillingCustomerName() {
        return billingCustomerName;
    }

    public void setBillingCustomerName(String billingCustomerName) {
        this.billingCustomerName = billingCustomerName;
    }

    public String getBillingLastName() {
        return billingLastName;
    }

    public void setBillingLastName(String billingLastName) {
        this.billingLastName = billingLastName;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getBillingAddress2() {
        return billingAddress2;
    }

    public void setBillingAddress2(String billingAddress2) {
        this.billingAddress2 = billingAddress2;
    }

    public String getBillingCity() {
        return billingCity;
    }

    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
    }

    public String getBillingPincode() {
        return billingPincode;
    }

    public void setBillingPincode(String billingPincode) {
        this.billingPincode = billingPincode;
    }

    public String getBillingState() {
        return billingState;
    }

    public void setBillingState(String billingState) {
        this.billingState = billingState;
    }

    public String getBillingCountry() {
        return billingCountry;
    }

    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public String getBillingPhone() {
        return billingPhone;
    }

    public void setBillingPhone(String billingPhone) {
        this.billingPhone = billingPhone;
    }

    public boolean isShippingIsBilling() {
        return shippingIsBilling;
    }

    public void setShippingIsBilling(boolean shippingIsBilling) {
        this.shippingIsBilling = shippingIsBilling;
    }

    public List<ShiprocketOrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<ShiprocketOrderItem> orderItems) {
        this.orderItems = orderItems;
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

    public Double getTransactionCharges() {
        return transactionCharges;
    }

    public void setTransactionCharges(Double transactionCharges) {
        this.transactionCharges = transactionCharges;
    }

    public Double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(Double totalDiscount) {
        this.totalDiscount = totalDiscount;
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

    // ── Inner class: Order Item ───────────────────────────────────────────
    public static class ShiprocketOrderItem {

        @JsonProperty("name")
        private String name;                // productName (+ color/size if variant)

        @JsonProperty("sku")
        private String sku;

        @JsonProperty("units")
        private Integer units;

        @JsonProperty("selling_price")
        private String sellingPrice;        // SR expects String

        @JsonProperty("discount")
        private String discount = "";

        @JsonProperty("tax")
        private String tax = "";

        @JsonProperty("hsn")
        private String hsn;

        // getters and setters ...


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public Integer getUnits() {
            return units;
        }

        public void setUnits(Integer units) {
            this.units = units;
        }

        public String getSellingPrice() {
            return sellingPrice;
        }

        public void setSellingPrice(String sellingPrice) {
            this.sellingPrice = sellingPrice;
        }

        public String getDiscount() {
            return discount;
        }

        public void setDiscount(String discount) {
            this.discount = discount;
        }

        public String getTax() {
            return tax;
        }

        public void setTax(String tax) {
            this.tax = tax;
        }

        public String getHsn() {
            return hsn;
        }

        public void setHsn(String hsn) {
            this.hsn = hsn;
        }
    }
}

