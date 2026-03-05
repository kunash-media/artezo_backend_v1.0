package com.artezo.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ProductResponseDto {

    private Long productId;                 // internal prime id
    private String productStrId;            // PRD0001, etc. (added for frontend convenience)

    private String productName;
    private String brandName;
    private String productCategory;
    private String productSubCategory;

    private String productVideoUrl;

    private boolean isDeleted;
    private boolean hasVariants;
    private boolean isCustomizable;
    private boolean isExchange;              // returning as String "true"/"false" to match your payload example

    // Currently selected variant root level
    private String currentSku;
    private String selectedColor;
    private Double currentSellingPrice;
    private Double currentMrpPrice;
    private Integer currentStock;

    private String mainImage;               // URL
    private List<String> mockupImages;      // URLs

    private List<String> description;
    private List<String> aboutItem;

    private Map<String, String> specifications;
    private Map<String, Object> additionalInfo;   // flexible – can hold String, numbers, etc.

    private Map<String, String> faq;


    private List<VariantResponseDto> availableVariants;

    // Added sections to match payload
    private List<HeroBannerResponseDto> heroBanners;
    private List<CouponResponseDto> availableCoupons;
    private List<ProductReviewResponseDto> productReviews;
    private List<InstallationStepResponseDto> installationSteps;

    private List<String> globalTags;
    private List<String> addonKeys;

    private List<String> categoryPath = new ArrayList<>();

    private String youtubeUrl;
    private boolean returnAvailable;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductStrId() {
        return productStrId;
    }

    public void setProductStrId(String productStrId) {
        this.productStrId = productStrId;
    }

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

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isHasVariants() {
        return hasVariants;
    }

    public void setHasVariants(boolean hasVariants) {
        this.hasVariants = hasVariants;
    }

    public boolean getIsCustomizable() {
        return isCustomizable;
    }

    public void setIsCustomizable(boolean customizable) {
        isCustomizable = customizable;
    }

    public boolean getIsExchange() {
        return isExchange;
    }

    public void setIsExchange(boolean exchange) {
        isExchange = exchange;
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

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public List<String> getMockupImages() {
        return mockupImages;
    }

    public void setMockupImages(List<String> mockupImages) {
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

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public List<VariantResponseDto> getAvailableVariants() {
        return availableVariants;
    }

    public void setAvailableVariants(List<VariantResponseDto> availableVariants) {
        this.availableVariants = availableVariants;
    }

    public List<HeroBannerResponseDto> getHeroBanners() {
        return heroBanners;
    }

    public void setHeroBanners(List<HeroBannerResponseDto> heroBanners) {
        this.heroBanners = heroBanners;
    }

    public List<CouponResponseDto> getAvailableCoupons() {
        return availableCoupons;
    }

    public void setAvailableCoupons(List<CouponResponseDto> availableCoupons) {
        this.availableCoupons = availableCoupons;
    }

    public List<ProductReviewResponseDto> getProductReviews() {
        return productReviews;
    }

    public void setProductReviews(List<ProductReviewResponseDto> productReviews) {
        this.productReviews = productReviews;
    }


    public List<InstallationStepResponseDto> getInstallationSteps() {
        return installationSteps;
    }

    public void setInstallationSteps(List<InstallationStepResponseDto> installationSteps) {
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

    public Map<String, String> getFaq() {
        return faq;
    }

    public void setFaq(Map<String, String> faq) {
        this.faq = faq;
    }

    public List<String> getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(List<String> categoryPath) {
        this.categoryPath = categoryPath;
    }

    public String getProductVideoUrl() {
        return productVideoUrl;
    }

    public void setProductVideoUrl(String productVideoUrl) {
        this.productVideoUrl = productVideoUrl;
    }

    public boolean getReturnAvailable() {
        return returnAvailable;
    }

    public void setReturnAvailable(boolean returnAvailable) {
        this.returnAvailable = returnAvailable;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }
}