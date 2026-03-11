package com.artezo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products_table")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productPrimeId;

    @Column(unique = true, nullable = false)
    private String productStrId;                        // PRD0001, PRD0002 ...

    private String productName;
    private String brandName;
    private String productCategory;
    private String productSubCategory;

    private boolean underTrendCategory;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    @Column(columnDefinition = "boolean default false")
    private boolean hasVariants;

    @Column(columnDefinition = "boolean default false")
    private boolean isCustomizable;

    @Column(columnDefinition = "boolean default false")
    private boolean isExchange;

    // Root level currently selected variant fields (denormalized for fast read)
    private String currentSku;
    private String selectedColor;
    private Double currentSellingPrice;
    private Double currentMrpPrice;
    private Integer currentStock;

    @Lob
    private byte[] mainImageData;

    @Lob
    @Column(name = "product_video_data", columnDefinition = "LONGBLOB")
    private byte[] productVideoData;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_mockup_images", joinColumns = @JoinColumn(name = "product_id"))
    @Lob
    private List<byte[]> mockupImageDataList = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "category_path_products", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "category_path")
    private List<String> categoryPath = new ArrayList<>();

    @Column(columnDefinition = "LONGTEXT")
    private String description;            // can be JSON or comma separated – or use @ElementCollection

    @Column(columnDefinition = "LONGTEXT")
    private String aboutItem;              // same

    @Column(columnDefinition = "JSON")
    private String specifications;         // JSON string (or make separate entity later)

    @Column(columnDefinition = "JSON")
    private String additionalInfo;         // JSON


    @Column(columnDefinition = "JSON")
    private String heroBanners;

    @Column(columnDefinition = "JSON")
    private String faq;

    @Column(columnDefinition = "JSON")
    private String globalTags;             // JSON array or comma separated

    @Column(columnDefinition = "JSON")
    private String addonKeys;              // JSON array

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariantEntity> variants = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "return_available", columnDefinition = "boolean default true")
    private boolean returnAvailable;

    @Column(columnDefinition = "JSON")
    private String customFields;

    public ProductEntity(){}

    public ProductEntity(Long productPrimeId, String productStrId, String productName, String brandName,
                         String productCategory, String productSubCategory,
                         boolean underTrendCategory, boolean isDeleted, boolean hasVariants,
                         boolean isCustomizable, boolean isExchange, String currentSku,
                         String selectedColor, Double currentSellingPrice, Double currentMrpPrice,
                         Integer currentStock, byte[] mainImageData, byte[] productVideoData,
                         List<byte[]> mockupImageDataList, List<String> categoryPath,
                         String description, String aboutItem, String specifications,
                         String additionalInfo, String heroBanners, String faq, String globalTags,
                         String addonKeys, List<ProductVariantEntity> variants, LocalDateTime createdAt,
                         LocalDateTime updatedAt, boolean returnAvailable, String youtubeUrl, String customFields) {
        this.productPrimeId = productPrimeId;
        this.productStrId = productStrId;
        this.productName = productName;
        this.brandName = brandName;
        this.productCategory = productCategory;
        this.productSubCategory = productSubCategory;
        this.underTrendCategory = underTrendCategory;
        this.isDeleted = isDeleted;
        this.hasVariants = hasVariants;
        this.isCustomizable = isCustomizable;
        this.isExchange = isExchange;
        this.currentSku = currentSku;
        this.selectedColor = selectedColor;
        this.currentSellingPrice = currentSellingPrice;
        this.currentMrpPrice = currentMrpPrice;
        this.currentStock = currentStock;
        this.mainImageData = mainImageData;
        this.productVideoData = productVideoData;
        this.mockupImageDataList = mockupImageDataList;
        this.categoryPath = categoryPath;
        this.description = description;
        this.aboutItem = aboutItem;
        this.specifications = specifications;
        this.additionalInfo = additionalInfo;
        this.heroBanners = heroBanners;
        this.faq = faq;
        this.globalTags = globalTags;
        this.addonKeys = addonKeys;
        this.variants = variants;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.returnAvailable = returnAvailable;
        this.youtubeUrl = youtubeUrl;
        this.customFields = customFields;
    }


    //Getter Setter

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

    public boolean getHasVariants() {
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

    public byte[] getMainImageData() {
        return mainImageData;
    }

    public void setMainImageData(byte[] mainImageData) {
        this.mainImageData = mainImageData;
    }


    public List<byte[]> getMockupImageDataList() {
        return mockupImageDataList;
    }

    public void setMockupImageDataList(List<byte[]> mockupImageDataList) {
        this.mockupImageDataList = mockupImageDataList;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAboutItem() {
        return aboutItem;
    }

    public void setAboutItem(String aboutItem) {
        this.aboutItem = aboutItem;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getGlobalTags() {
        return globalTags;
    }

    public void setGlobalTags(String globalTags) {
        this.globalTags = globalTags;
    }

    public String getAddonKeys() {
        return addonKeys;
    }

    public void setAddonKeys(String addonKeys) {
        this.addonKeys = addonKeys;
    }

    public List<ProductVariantEntity> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariantEntity> variants) {
        this.variants = variants;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean getUnderTrendCategory() {
        return underTrendCategory;
    }

    public void setUnderTrendCategory(boolean underTrendCategory) {
        this.underTrendCategory = underTrendCategory;
    }

    public String getFaq() {
        return faq;
    }

    public void setFaq(String faq) {
        this.faq = faq;
    }

    public String getHeroBanners() {
        return heroBanners;
    }

    public void setHeroBanners(String heroBanners) {
        this.heroBanners = heroBanners;
    }

    public List<String> getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(List<String> categoryPath) {
        this.categoryPath = categoryPath;
    }

    public byte[] getProductVideoData() {
        return productVideoData;
    }

    public void setProductVideoData(byte[] productVideoData) {
        this.productVideoData = productVideoData;
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

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }
}