package com.artezo.dto.request;


public class InventoryProductDTO {

    private Long productPrimeId;
    private String productStrId;
    private String title;
    private String currentSku;
    private String productCategory;
    private String productSubCategory;

    private String variantTitle;
    private Integer variantStock;
    private Integer currentStock;       // ← ADD THIS

    // InventoryProductDTO
    private boolean isVariantStock;     // true = variant stock, false = root product stock
    private String variantId;           // which variant this inventory belongs to
    // Add any other product fields you need

    public InventoryProductDTO(){}

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getVariantTitle() {
        return variantTitle;
    }

    public void setVariantTitle(String variantTitle) {
        this.variantTitle = variantTitle;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public boolean getIsVariantStock() {
        return isVariantStock;
    }

    public void setIsVariantStock(boolean variantStock) {
        isVariantStock = variantStock;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public Integer getVariantStock() {
        return variantStock;
    }

    public void setVariantStock(Integer variantStock) {
        this.variantStock = variantStock;
    }
}