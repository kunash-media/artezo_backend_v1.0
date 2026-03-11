package com.artezo.dto.request;

import lombok.Builder;


@Builder
public class ProductCardSnapshotDto {

    private Long productPrimeId;            // 1
    private String productStrId;            // "PRD00001"
    private String productName;             // "Artezo Silver Acrylic..."
    private String brandName;               // "Artezo"
    private String mainImage;               // "/api/products/1/main"
    private Double currentSellingPrice;     // 499.0
    private Double currentMrpPrice;         // 899.0
    private String currentSku;              // "ART-WPLATE-GLD"
    private String productCategory;         // "Plate"
    private String productSubCategory;      // "Wall Plates"
    private Integer currentStock;           // 20
    private Boolean hasVariants;            // true
    private String selectedColor;           // "Golden"
    private Long viewedAt;                  // epoch timestamp


    public ProductCardSnapshotDto(){}

    public ProductCardSnapshotDto(Long productPrimeIdId, String productStrId, String productName, String brandName, String mainImage, Double currentSellingPrice, Double currentMrpPrice, String currentSku, String productCategory, String productSubCategory, Integer currentStock, Boolean hasVariants, String selectedColor, Long viewedAt) {
        this.productPrimeId = productPrimeIdId;
        this.productStrId = productStrId;
        this.productName = productName;
        this.brandName = brandName;
        this.mainImage = mainImage;
        this.currentSellingPrice = currentSellingPrice;
        this.currentMrpPrice = currentMrpPrice;
        this.currentSku = currentSku;
        this.productCategory = productCategory;
        this.productSubCategory = productSubCategory;
        this.currentStock = currentStock;
        this.hasVariants = hasVariants;
        this.selectedColor = selectedColor;
        this.viewedAt = viewedAt;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getProductPrimeId() {
        return productPrimeId;
    }

    public void setProductPrimeId(Long productPrimeId) {
        this.productPrimeId = productPrimeId;
    }

    public String getProductStrId() {
        return productStrId;
    }

    public void setProductStrId(String productStrId) {
        this.productStrId = productStrId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public Double getCurrentSellingPrice() {
        return currentSellingPrice;
    }

    public void setCurrentSellingPrice(Double currentSellingPrice) {
        this.currentSellingPrice = currentSellingPrice;
    }

    public Double getCurrentMrpPrice() {
        return currentMrpPrice;
    }

    public void setCurrentMrpPrice(Double currentMrpPrice) {
        this.currentMrpPrice = currentMrpPrice;
    }

    public String getCurrentSku() {
        return currentSku;
    }

    public void setCurrentSku(String currentSku) {
        this.currentSku = currentSku;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(String productSubCategory) {
        this.productSubCategory = productSubCategory;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Boolean getHasVariants() {
        return hasVariants;
    }

    public void setHasVariants(Boolean hasVariants) {
        this.hasVariants = hasVariants;
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
    }

    public Long getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Long viewedAt) {
        this.viewedAt = viewedAt;
    }
}