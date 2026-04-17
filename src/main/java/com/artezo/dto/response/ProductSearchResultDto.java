package com.artezo.dto.response;


public class ProductSearchResultDto {

    private Long productPrimeId;
    private String productStrId;
    private String productName;
    private String brandName;
    private Double currentSellingPrice;
    private Double currentMrpPrice;
    private String mainImageUrl;       // e.g. /api/products/{id}/main
    private String productCategory;
    private String productSubCategory;

    public ProductSearchResultDto() {}

    public ProductSearchResultDto(Long productPrimeId, String productStrId,
                                  String productName, String brandName,
                                  Double currentSellingPrice, Double currentMrpPrice,
                                  String productCategory, String productSubCategory) {
        this.productPrimeId = productPrimeId;
        this.productStrId = productStrId;
        this.productName = productName;
        this.brandName = brandName;
        this.currentSellingPrice = currentSellingPrice;
        this.currentMrpPrice = currentMrpPrice;
        this.productCategory = productCategory;
        this.productSubCategory = productSubCategory;
        this.mainImageUrl = "/api/products/" + productPrimeId + "/main";
    }

    // --- Getters & Setters ---

    public Long getProductPrimeId() { return productPrimeId; }
    public void setProductPrimeId(Long productPrimeId) { this.productPrimeId = productPrimeId; }

    public String getProductStrId() { return productStrId; }
    public void setProductStrId(String productStrId) { this.productStrId = productStrId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public Double getCurrentSellingPrice() { return currentSellingPrice; }
    public void setCurrentSellingPrice(Double currentSellingPrice) { this.currentSellingPrice = currentSellingPrice; }

    public Double getCurrentMrpPrice() { return currentMrpPrice; }
    public void setCurrentMrpPrice(Double currentMrpPrice) { this.currentMrpPrice = currentMrpPrice; }

    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public String getProductSubCategory() { return productSubCategory; }
    public void setProductSubCategory(String productSubCategory) { this.productSubCategory = productSubCategory; }
}