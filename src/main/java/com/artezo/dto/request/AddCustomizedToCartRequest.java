package com.artezo.dto.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request for POST /api/v1/cart/add-customized
 * Extends normal cart add with assetUuid from upload step.
 * Existing AddToCartRequest is NOT modified.
 */
public class AddCustomizedToCartRequest {

    // ── Same fields as AddToCartRequest ───────────────────────────────────────
    private Long userId;
    private String sessionId;
    private Long productId;
    private String variantId;
    private String sku;
    private String selectedColor;
    private String selectedSize;
    private String titleName;
    private String productName;
    private BigDecimal unitPrice;
    private BigDecimal mrpPrice;
    private Integer quantity;
    private String customFieldsJson;

    /**
     * Ordered list of uploaded asset UUIDs.
     * slot 1 = primary image (shown in cart/order)
     * slot 2,3 = additional frames
     */
    private List<AssetSlotRequest> assetSlots;      // returned by /customize/upload-image


    // ── ADD inner class ───────────────────────────────────────────────────────
    public static class AssetSlotRequest {
        private Integer slotNumber;   // 1, 2, 3...
        private String assetUuid;    // UUID from upload response
        private String fieldName;    // "upload image", "upload image - 2"

        public Integer getSlotNumber() {
            return slotNumber;
        }

        public void setSlotNumber(Integer slotNumber) {
            this.slotNumber = slotNumber;
        }

        public  String getAssetUuid() {
            return assetUuid;
        }

        public void setAssetUuid(String assetUuid) {
            this.assetUuid = assetUuid;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public String getCustomFieldsJson() {
        return customFieldsJson;
    }

    public void setCustomFieldsJson(String customFieldsJson) {
        this.customFieldsJson = customFieldsJson;
    }


    public List<AssetSlotRequest> getAssetSlots() {
        return assetSlots;
    }

    public void setAssetSlots(List<AssetSlotRequest> assetSlots) {
        this.assetSlots = assetSlots;
    }

}