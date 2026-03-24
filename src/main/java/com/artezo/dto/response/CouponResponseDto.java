package com.artezo.dto.response;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class CouponResponseDto {

    private static final Logger LOGGER = Logger.getLogger(CouponResponseDto.class.getName());

    private Long couponId;
    private String couponCode;
    private String description;
    private String discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer usageLimit;
    private Integer usagePerCustomer;
    private Integer usedCount;
    private Boolean isActive;
    private Boolean excludeSaleItems;
    private Boolean freeShipping;
    private LocalDateTime createdAt;
    private Boolean couponUsed;

    public CouponResponseDto() {
        LOGGER.fine("CouponResponseDto empty constructor called");
    }

    public CouponResponseDto(Long couponId, String couponCode, String description, String discountType,
                             Double discountValue, Double minOrderAmount, Double maxDiscountAmount,
                             LocalDateTime validFrom, LocalDateTime validTo, Integer usageLimit,
                             Integer usagePerCustomer, Integer usedCount, Boolean isActive,
                             Boolean excludeSaleItems, Boolean freeShipping, LocalDateTime createdAt, Boolean couponUsed) {
        LOGGER.fine("CouponResponseDto parameterized constructor called for couponId: " + couponId + ", couponCode: " + couponCode);
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
        LOGGER.fine("CouponResponseDto created for coupon: " + this.couponCode);
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getCouponCode() {
        LOGGER.finest("Getting couponCode from response DTO");
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        LOGGER.fine("Setting couponCode in response DTO to: " + couponCode);
        this.couponCode = couponCode;
    }

    public String getDescription() {
        LOGGER.finest("Getting description from response DTO for coupon: " + couponCode);
        return description;
    }

    public void setDescription(String description) {
        LOGGER.fine("Setting description in response DTO for coupon " + couponCode + " to: " + description);
        this.description = description;
    }

    public String getDiscountType() {
        LOGGER.finest("Getting discountType from response DTO for coupon: " + couponCode);
        return discountType;
    }

    public void setDiscountType(String discountType) {
        LOGGER.fine("Setting discountType in response DTO for coupon " + couponCode + " to: " + discountType);
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        LOGGER.finest("Getting discountValue from response DTO for coupon: " + couponCode);
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        LOGGER.fine("Setting discountValue in response DTO for coupon " + couponCode + " to: " + discountValue);
        this.discountValue = discountValue;
    }

    public Double getMinOrderAmount() {
        LOGGER.finest("Getting minOrderAmount from response DTO for coupon: " + couponCode);
        return minOrderAmount;
    }

    public void setMinOrderAmount(Double minOrderAmount) {
        LOGGER.fine("Setting minOrderAmount in response DTO for coupon " + couponCode + " to: " + minOrderAmount);
        this.minOrderAmount = minOrderAmount;
    }

    public Double getMaxDiscountAmount() {
        LOGGER.finest("Getting maxDiscountAmount from response DTO for coupon: " + couponCode);
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        LOGGER.fine("Setting maxDiscountAmount in response DTO for coupon " + couponCode + " to: " + maxDiscountAmount);
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public LocalDateTime getValidFrom() {
        LOGGER.finest("Getting validFrom from response DTO for coupon: " + couponCode);
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        LOGGER.fine("Setting validFrom in response DTO for coupon " + couponCode + " to: " + validFrom);
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        LOGGER.finest("Getting validTo from response DTO for coupon: " + couponCode);
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        LOGGER.fine("Setting validTo in response DTO for coupon " + couponCode + " to: " + validTo);
        this.validTo = validTo;
    }

    public Integer getUsageLimit() {
        LOGGER.finest("Getting usageLimit from response DTO for coupon: " + couponCode);
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        LOGGER.fine("Setting usageLimit in response DTO for coupon " + couponCode + " to: " + usageLimit);
        this.usageLimit = usageLimit;
    }

    public Integer getUsagePerCustomer() {
        LOGGER.finest("Getting usagePerCustomer from response DTO for coupon: " + couponCode);
        return usagePerCustomer;
    }

    public void setUsagePerCustomer(Integer usagePerCustomer) {
        LOGGER.fine("Setting usagePerCustomer in response DTO for coupon " + couponCode + " to: " + usagePerCustomer);
        this.usagePerCustomer = usagePerCustomer;
    }

    public Integer getUsedCount() {
        LOGGER.finest("Getting usedCount from response DTO for coupon: " + couponCode);
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        LOGGER.fine("Setting usedCount in response DTO for coupon " + couponCode + " to: " + usedCount);
        this.usedCount = usedCount;
    }

    public Boolean getIsActive() {
        LOGGER.finest("Getting isActive from response DTO for coupon: " + couponCode);
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        LOGGER.fine("Setting isActive in response DTO for coupon " + couponCode + " to: " + isActive);
        this.isActive = isActive;
    }

    public Boolean getExcludeSaleItems() {
        LOGGER.finest("Getting excludeSaleItems from response DTO for coupon: " + couponCode);
        return excludeSaleItems;
    }

    public void setExcludeSaleItems(Boolean excludeSaleItems) {
        LOGGER.fine("Setting excludeSaleItems in response DTO for coupon " + couponCode + " to: " + excludeSaleItems);
        this.excludeSaleItems = excludeSaleItems;
    }

    public Boolean getFreeShipping() {
        LOGGER.finest("Getting freeShipping from response DTO for coupon: " + couponCode);
        return freeShipping;
    }

    public void setFreeShipping(Boolean freeShipping) {
        LOGGER.fine("Setting freeShipping in response DTO for coupon " + couponCode + " to: " + freeShipping);
        this.freeShipping = freeShipping;
    }

    public LocalDateTime getCreatedAt() {
        LOGGER.finest("Getting createdAt from response DTO for coupon: " + couponCode);
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        LOGGER.fine("Setting createdAt in response DTO for coupon " + couponCode + " to: " + createdAt);
        this.createdAt = createdAt;
    }

    public Boolean getCouponUsed() {
        return couponUsed;
    }

    public void setCouponUsed(Boolean couponUsed) {
        this.couponUsed = couponUsed;
    }
}