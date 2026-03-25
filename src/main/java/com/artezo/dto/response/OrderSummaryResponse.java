package com.artezo.dto.response;

import java.time.LocalDateTime;
import java.util.List;


public class OrderSummaryResponse {

    private String orderStrId;
    private LocalDateTime orderDate;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentMode;

    private String customerName;
    private String customerPhone;

    private Double subTotal;
    private Double discountAmount;
    private Double discountPercent;
    private Double tax;
    private Double finalAmount;

    private String courierName;
    private String orderNotes;

    private boolean returnRequested;
    private boolean exchangeRequested;

    private List<OrderItemSummary> orderItems;


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

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getOrderNotes() {
        return orderNotes;
    }

    public void setOrderNotes(String orderNotes) {
        this.orderNotes = orderNotes;
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

    public List<OrderItemSummary> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemSummary> orderItems) {
        this.orderItems = orderItems;
    }


    public static class OrderItemSummary {
        private String productName;
        private String color;
        private Integer quantity;
        private Double sellingPrice;
        private Double itemTotal;
        private String productImageUrl;

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getSellingPrice() {
            return sellingPrice;
        }

        public void setSellingPrice(Double sellingPrice) {
            this.sellingPrice = sellingPrice;
        }

        public Double getItemTotal() {
            return itemTotal;
        }

        public void setItemTotal(Double itemTotal) {
            this.itemTotal = itemTotal;
        }

        public String getProductImageUrl() {
            return productImageUrl;
        }

        public void setProductImageUrl(String productImageUrl) {
            this.productImageUrl = productImageUrl;
        }
    }
}