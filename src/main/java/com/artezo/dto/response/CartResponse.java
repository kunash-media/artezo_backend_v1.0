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


    // ── Add this inner class to CartResponse.java ─────────────────────────────
    public static class AssetSlotInfo {
        private Integer slotNumber;
        private String assetUuid;
        private String imageUrl;      // /api/v1/customize/image/{uuid}
        private String fieldName;

        public AssetSlotInfo() {}
        public AssetSlotInfo(Integer slotNumber, String assetUuid,
                             String imageUrl, String fieldName) {
            this.slotNumber  = slotNumber;
            this.assetUuid   = assetUuid;
            this.imageUrl    = imageUrl;
            this.fieldName   = fieldName;
        }

        public Integer getSlotNumber()  { return slotNumber; }
        public String  getAssetUuid()   { return assetUuid; }
        public String  getImageUrl()    { return imageUrl; }
        public String  getFieldName()   { return fieldName; }

        public void setSlotNumber(Integer slotNumber)  { this.slotNumber = slotNumber; }
        public void setAssetUuid(String assetUuid)     { this.assetUuid  = assetUuid; }
        public void setImageUrl(String imageUrl)        { this.imageUrl   = imageUrl; }
        public void setFieldName(String fieldName)      { this.fieldName  = fieldName; }
    }



    @Builder
    public static class CartItemResponse {
        private Long itemId;
        private Long productId;
        private String productStrId;
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

        private String productCategory; // ← just add here, no DB change


        private String productImageUrl;

        // ── Inside CartItemResponse — ADD this field ──────────────────────────────
        // After existing productImageUrl field:
        private List<CartResponse.AssetSlotInfo> customizationSlots; // all uploaded images


        public Long getItemId() {
            return itemId;
        }

        public String getProductStrId() {
            return productStrId;
        }

        public void setProductStrId(String productStrId) {
            this.productStrId = productStrId;
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

        public String getProductCategory() {
            return productCategory;
        }

        public void setProductCategory(String productCategory) {
            this.productCategory = productCategory;
        }

        public List<CartResponse.AssetSlotInfo> getCustomizationSlots() {
            return customizationSlots;
        }
        public void setCustomizationSlots(
                List<CartResponse.AssetSlotInfo> customizationSlots) {
            this.customizationSlots = customizationSlots;
        }
    }
}