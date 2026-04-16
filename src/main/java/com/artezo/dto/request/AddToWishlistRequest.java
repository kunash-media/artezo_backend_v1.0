package com.artezo.dto.request;

import lombok.Data;
import java.math.BigDecimal;

public class AddToWishlistRequest {
    private Long userId;
    private String wishlistName;     // default: "My Wishlist"
    private Long productId;
    private String variantId;
    private String sku;
    private String selectedColor;
    private String selectedSize;
    private String titleName;
    private BigDecimal wishlistedPrice;
    private String customFieldsJson;
    private String productName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getWishlistName() {
        return wishlistName;
    }

    public void setWishlistName(String wishlistName) {
        this.wishlistName = wishlistName;
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

    public BigDecimal getWishlistedPrice() {
        return wishlistedPrice;
    }

    public void setWishlistedPrice(BigDecimal wishlistedPrice) {
        this.wishlistedPrice = wishlistedPrice;
    }

    public String getCustomFieldsJson() {
        return customFieldsJson;
    }

    public void setCustomFieldsJson(String customFieldsJson) {
        this.customFieldsJson = customFieldsJson;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}