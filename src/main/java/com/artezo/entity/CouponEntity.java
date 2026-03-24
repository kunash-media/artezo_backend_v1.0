package com.artezo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@Entity
@Table(name = "coupons")
public class CouponEntity {

    private static final Logger LOGGER = Logger.getLogger(CouponEntity.class.getName());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false)
    private String couponCode;

    private String description;

    @Column(name = "discount_type")
    private String discountType;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;


    public CouponEntity() {
        LOGGER.fine("CouponEntity empty constructor called");
    }

    public CouponEntity(Long couponId, String couponCode, String description, String discountType,
                        Double discountValue, Double minOrderAmount, Double maxDiscountAmount,
                        LocalDateTime validFrom, LocalDateTime validTo, Integer usageLimit,
                        Integer usagePerCustomer, Integer usedCount, Boolean isActive,
                        Boolean excludeSaleItems, Boolean freeShipping, LocalDateTime createdAt,
                        Boolean couponUsed, UserEntity user) {
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.usageLimit = usageLimit;
        this.usagePerCustomer = usagePerCustomer;
        this.usedCount = usedCount;
        this.isActive = isActive;
        this.excludeSaleItems = excludeSaleItems;
        this.freeShipping = freeShipping;
        this.createdAt = createdAt;
        this.couponUsed = couponUsed;
        this.user = user;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }


    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getDescription() {
        LOGGER.finest("Getting description for coupon: " + couponCode);
        return description;
    }

    public void setDescription(String description) {
        LOGGER.fine("Setting description for coupon " + couponCode + " to: " + description);
        this.description = description;
    }

    public String getDiscountType() {
        LOGGER.finest("Getting discountType for coupon: " + couponCode);
        return discountType;
    }

    public void setDiscountType(String discountType) {
        LOGGER.fine("Setting discountType for coupon " + couponCode + " to: " + discountType);
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        LOGGER.finest("Getting discountValue for coupon: " + couponCode);
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        LOGGER.fine("Setting discountValue for coupon " + couponCode + " to: " + discountValue);
        this.discountValue = discountValue;
    }

    public Double getMinOrderAmount() {
        LOGGER.finest("Getting minOrderAmount for coupon: " + couponCode);
        return minOrderAmount;
    }

    public void setMinOrderAmount(Double minOrderAmount) {
        LOGGER.fine("Setting minOrderAmount for coupon " + couponCode + " to: " + minOrderAmount);
        this.minOrderAmount = minOrderAmount;
    }

    public Double getMaxDiscountAmount() {
        LOGGER.finest("Getting maxDiscountAmount for coupon: " + couponCode);
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        LOGGER.fine("Setting maxDiscountAmount for coupon " + couponCode + " to: " + maxDiscountAmount);
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public LocalDateTime getValidFrom() {
        LOGGER.finest("Getting validFrom for coupon: " + couponCode);
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        LOGGER.fine("Setting validFrom for coupon " + couponCode + " to: " + validFrom);
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        LOGGER.finest("Getting validTo for coupon: " + couponCode);
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        LOGGER.fine("Setting validTo for coupon " + couponCode + " to: " + validTo);
        this.validTo = validTo;
    }

    public Integer getUsageLimit() {
        LOGGER.finest("Getting usageLimit for coupon: " + couponCode);
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        LOGGER.fine("Setting usageLimit for coupon " + couponCode + " to: " + usageLimit);
        this.usageLimit = usageLimit;
    }

    public Integer getUsagePerCustomer() {
        LOGGER.finest("Getting usagePerCustomer for coupon: " + couponCode);
        return usagePerCustomer;
    }

    public void setUsagePerCustomer(Integer usagePerCustomer) {
        LOGGER.fine("Setting usagePerCustomer for coupon " + couponCode + " to: " + usagePerCustomer);
        this.usagePerCustomer = usagePerCustomer;
    }

    public Integer getUsedCount() {
        LOGGER.finest("Getting usedCount for coupon: " + couponCode);
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        LOGGER.fine("Setting usedCount for coupon " + couponCode + " to: " + usedCount);
        this.usedCount = usedCount;
    }

    public Boolean getIsActive() {
        LOGGER.finest("Getting isActive for coupon: " + couponCode);
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        LOGGER.fine("Setting isActive for coupon " + couponCode + " to: " + isActive);
        this.isActive = isActive;
    }

    public Boolean getExcludeSaleItems() {
        LOGGER.finest("Getting excludeSaleItems for coupon: " + couponCode);
        return excludeSaleItems;
    }

    public void setExcludeSaleItems(Boolean excludeSaleItems) {
        LOGGER.fine("Setting excludeSaleItems for coupon " + couponCode + " to: " + excludeSaleItems);
        this.excludeSaleItems = excludeSaleItems;
    }

    public Boolean getFreeShipping() {
        LOGGER.finest("Getting freeShipping for coupon: " + couponCode);
        return freeShipping;
    }

    public void setFreeShipping(Boolean freeShipping) {
        LOGGER.fine("Setting freeShipping for coupon " + couponCode + " to: " + freeShipping);
        this.freeShipping = freeShipping;
    }

    public LocalDateTime getCreatedAt() {
        LOGGER.finest("Getting createdAt for coupon: " + couponCode);
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        LOGGER.fine("Setting createdAt for coupon " + couponCode + " to: " + createdAt);
        this.createdAt = createdAt;
    }

    public Boolean getCouponUsed() {
        return couponUsed;
    }

    public void setCouponUsed(Boolean couponUsed) {
        this.couponUsed = couponUsed;
    }


    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}