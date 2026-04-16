package com.artezo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Builder
public class CartResponse {
    private Long cartId;
    private Long userId;
    private String status;
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal totalAmount;
    private BigDecimal totalMrp;
    private BigDecimal totalDiscount;

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalMrp() {
        return totalMrp;
    }

    public void setTotalMrp(BigDecimal totalMrp) {
        this.totalMrp = totalMrp;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
    }


    @Builder
    public static class CartItemResponse {
        private Long itemId;
        private Long productId;
        private String variantId;
        private String sku;
        private String selectedColor;
        private String selectedSize;
        private String titleName;
        private BigDecimal unitPrice;
        private BigDecimal mrpPrice;
        private Integer quantity;
        private BigDecimal itemTotal;
        private String customFieldsJson;
        private LocalDateTime createdAt;

        private String productImageUrl;


        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getVariantId() {
            return variantId;
        }

        public void setVariantId(String variantId) {
            this.variantId = variantId;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getSelectedColor() {
            return selectedColor;
        }

        public void setSelectedColor(String selectedColor) {
            this.selectedColor = selectedColor;
        }

        public String getSelectedSize() {
            return selectedSize;
        }

        public void setSelectedSize(String selectedSize) {
            this.selectedSize = selectedSize;
        }

        public String getTitleName() {
            return titleName;
        }

        public void setTitleName(String titleName) {
            this.titleName = titleName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getMrpPrice() {
            return mrpPrice;
        }

        public void setMrpPrice(BigDecimal mrpPrice) {
            this.mrpPrice = mrpPrice;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getItemTotal() {
            return itemTotal;
        }

        public void setItemTotal(BigDecimal itemTotal) {
            this.itemTotal = itemTotal;
        }

        public String getCustomFieldsJson() {
            return customFieldsJson;
        }

        public void setCustomFieldsJson(String customFieldsJson) {
            this.customFieldsJson = customFieldsJson;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public String getProductImageUrl() {
            return productImageUrl;
        }

        public void setProductImageUrl(String productImageUrl) {
            this.productImageUrl = productImageUrl;
        }
    }
}