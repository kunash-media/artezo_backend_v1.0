package com.artezo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupons")
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false)
    private String couponCode;

    private String description;

    @Column(name = "discount_type")
    private String discountType; // "PERCENTAGE" or "FLAT"

    @Column(name = "discount_value")
    private Double discountValue;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_per_customer")
    private Integer usagePerCustomer;

    @Column(name = "used_count")
    private Integer usedCount;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "exclude_sale_items")
    private Boolean excludeSaleItems;

    @Column(name = "free_shipping")
    private Boolean freeShipping;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "coupon_used")
    private Boolean couponUsed = false;

    // NEW: coupon type
    @Column(name = "coupon_type")
    private String couponType;

    // NEW: category name
    @Column(name = "category_name")
    private String categoryName;

    // NEW: many products link (Type 1 — specific products)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_products",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<ProductEntity> products = new ArrayList<>();

    // Variant-level specificity (Type 2 — specific SKUs/variants within a product)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_product_variants",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "variant_db_id")
    )
    private List<ProductVariantEntity> allowedVariants = new ArrayList<>();

    // NEW: allowed users list
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_allowed_users",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserEntity> allowedUsers = new ArrayList<>();

    // ---- Constructors ----
    public CouponEntity() {}

    // ---- Getters & Setters ----
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }

    public Double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public Double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(Double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsagePerCustomer() { return usagePerCustomer; }
    public void setUsagePerCustomer(Integer usagePerCustomer) { this.usagePerCustomer = usagePerCustomer; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getExcludeSaleItems() { return excludeSaleItems; }
    public void setExcludeSaleItems(Boolean excludeSaleItems) { this.excludeSaleItems = excludeSaleItems; }

    public Boolean getFreeShipping() { return freeShipping; }
    public void setFreeShipping(Boolean freeShipping) { this.freeShipping = freeShipping; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getCouponUsed() { return couponUsed; }
    public void setCouponUsed(Boolean couponUsed) { this.couponUsed = couponUsed; }

    public String getCouponType() { return couponType; }
    public void setCouponType(String couponType) { this.couponType = couponType; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public List<ProductEntity> getProducts() { return products; }
    public void setProducts(List<ProductEntity> products) { this.products = products; }

    public List<UserEntity> getAllowedUsers() { return allowedUsers; }
    public void setAllowedUsers(List<UserEntity> allowedUsers) { this.allowedUsers = allowedUsers; }



    public List<ProductVariantEntity> getAllowedVariants() {
        return allowedVariants;
    }

    public void setAllowedVariants(List<ProductVariantEntity> allowedVariants) {
        this.allowedVariants = allowedVariants;
    }
}