// com/artezo/dto/response/ProductVariantSummaryDto.java
package com.artezo.dto.response;

import java.util.List;

public class ProductVariantSummaryDto {

    private Long productPrimeId;
    private String productStrId;
    private String productName;
    private String productCategory;
    private String productSubCategory;
    private String mainImageUrl;   // was mainImageBase64

    private List<VariantSummary> variants;

    public static class VariantSummary {
        private Long variantDbId;            // ProductVariantEntity.id (used as FK in join table)
        private String variantId;            // "VAR-GOLD" etc
        private String color;
        private String size;
        private Double price;
        private Double mrp;
        private Integer stock;
        private String mainImageUrl;

        // getters/setters
        public Long getVariantDbId() { return variantDbId; }
        public void setVariantDbId(Long variantDbId) { this.variantDbId = variantDbId; }
        public String getVariantId() { return variantId; }
        public void setVariantId(String variantId) { this.variantId = variantId; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Double getMrp() { return mrp; }
        public void setMrp(Double mrp) { this.mrp = mrp; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public String getMainImageUrl() { return mainImageUrl; }
        public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }
    }

    // getters/setters
    public Long getProductPrimeId() { return productPrimeId; }
    public void setProductPrimeId(Long productPrimeId) { this.productPrimeId = productPrimeId; }
    public String getProductStrId() { return productStrId; }
    public void setProductStrId(String productStrId) { this.productStrId = productStrId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
    public String getProductSubCategory() { return productSubCategory; }
    public void setProductSubCategory(String productSubCategory) { this.productSubCategory = productSubCategory; }

    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }


    public List<VariantSummary> getVariants() { return variants; }
    public void setVariants(List<VariantSummary> variants) { this.variants = variants; }
}