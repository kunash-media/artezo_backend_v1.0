package com.artezo.dto.request;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CreateProductRequestDto {


    private String productName;
    private String brandName;
    private String productCategory;
    private String productSubCategory;

    private Boolean hasVariants;
    private Boolean isCustomizable;
    private Boolean isExchange;           // boolean – frontend can send true/false
    private Boolean returnAvailable;
    private Boolean underTrendCategory;

    // Currently selected / default variant data (root level)
    private String currentSku;
    private String selectedColor;
    private Double currentSellingPrice;
    private Double currentMrpPrice;
    private Integer currentStock;

    private byte[] mainImage;
    private List<byte[]> mockupImages;

    private byte[] productVideo;

    private List<String> description;           // bullet points
    private List<String> aboutItem;

    private Map<String, String> specifications;
    private Map<String, String> additionalInfo;

    private Map<String, String> faq;

    // Variants (only sent if hasVariants = true)
    private List<VariantRequestDto> variants;

    // New sections from payload
    private List<HeroBannerRequestDto> heroBanners;
    private List<CouponRequestDto> availableCoupons;
    private List<ProductReviewRequestDto> productReviews;
    private List<InstallationStepRequestDto> installationSteps;

    private List<String> globalTags;
    private List<String> addonKeys;

    private List<String> categoryPath = new ArrayList<>();
    private String youtubeUrl;

    private String customFields;

    private String hsnCode;
    private Double weight;
    private Double length;
    private Double breadth;
    private Double height;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
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




    public String getCurrentSku() {
        return currentSku;
    }

    public void setCurrentSku(String currentSku) {
        this.currentSku = currentSku;
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
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

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public byte[] getMainImage() {
        return mainImage;
    }

    public void setMainImage(byte[] mainImage) {
        this.mainImage = mainImage;
    }

    public List<byte[]> getMockupImages() {
        return mockupImages;
    }

    public void setMockupImages(List<byte[]> mockupImages) {
        this.mockupImages = mockupImages;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public List<String> getAboutItem() {
        return aboutItem;
    }

    public void setAboutItem(List<String> aboutItem) {
        this.aboutItem = aboutItem;
    }

    public Map<String, String> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, String> specifications) {
        this.specifications = specifications;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public Map<String, String> getFaq() {
        return faq;
    }

    public void setFaq(Map<String, String> faq) {
        this.faq = faq;
    }

    public List<VariantRequestDto> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantRequestDto> variants) {
        this.variants = variants;
    }

    public List<HeroBannerRequestDto> getHeroBanners() {
        return heroBanners;
    }

    public void setHeroBanners(List<HeroBannerRequestDto> heroBanners) {
        this.heroBanners = heroBanners;
    }

    public List<CouponRequestDto> getAvailableCoupons() {
        return availableCoupons;
    }

    public void setAvailableCoupons(List<CouponRequestDto> availableCoupons) {
        this.availableCoupons = availableCoupons;
    }

    public List<ProductReviewRequestDto> getProductReviews() {
        return productReviews;
    }

    public void setProductReviews(List<ProductReviewRequestDto> productReviews) {
        this.productReviews = productReviews;
    }


    public List<InstallationStepRequestDto> getInstallationSteps() {
        return installationSteps;
    }

    public void setInstallationSteps(List<InstallationStepRequestDto> installationSteps) {
        this.installationSteps = installationSteps;
    }

    public List<String> getGlobalTags() {
        return globalTags;
    }

    public void setGlobalTags(List<String> globalTags) {
        this.globalTags = globalTags;
    }

    public List<String> getAddonKeys() {
        return addonKeys;
    }

    public void setAddonKeys(List<String> addonKeys) {
        this.addonKeys = addonKeys;
    }

    public List<String> getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(List<String> categoryPath) {
        this.categoryPath = categoryPath;
    }

    public byte[] getProductVideo() {
        return productVideo;
    }

    public void setProductVideo(byte[] productVideo) {
        this.productVideo = productVideo;
    }


    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }


    public Boolean getIsCustomizable() {
        return isCustomizable;
    }

    public void setIsCustomizable(Boolean customizable) {
        isCustomizable = customizable;
    }

    public Boolean getIsExchange() {
        return isExchange;
    }

    public void setIsExchange(Boolean exchange) {
        isExchange = exchange;
    }

    public Boolean getHasVariants() {  // rename from hasVariants() to getHasVariants()
        return hasVariants;
    }

    public void setHasVariants(Boolean hasVariants) {
        this.hasVariants = hasVariants;
    }

    public Boolean getReturnAvailable() {
        return returnAvailable;
    }

    public void setReturnAvailable(Boolean returnAvailable) {
        this.returnAvailable = returnAvailable;
    }

    public Boolean getUnderTrendCategory() {
        return underTrendCategory;
    }

    public void setUnderTrendCategory(Boolean underTrendCategory) {
        this.underTrendCategory = underTrendCategory;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getBreadth() {
        return breadth;
    }

    public void setBreadth(Double breadth) {
        this.breadth = breadth;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }
}