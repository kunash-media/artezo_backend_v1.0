package com.artezo.dto.request;


public class InventoryProductDTO {

    private Long productPrimeId;
    private String productStrId;
    private String title;
    private String currentSku;
    private String productCategory;
    private String productSubCategory;

   private String variantTitle;

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
}