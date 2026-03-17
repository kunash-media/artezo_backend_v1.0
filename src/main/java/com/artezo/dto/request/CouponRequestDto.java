package com.artezo.dto.request;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class CouponRequestDto {

    private static final Logger LOGGER = Logger.getLogger(CouponRequestDto.class.getName());

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
    private Boolean isActive;
    private Boolean excludeSaleItems;
    private Boolean freeShipping;
    private Boolean couponUsed = false;

    public CouponRequestDto() {
        LOGGER.fine("CouponRequestDto empty constructor called");
    }

    public CouponRequestDto(String code, String description, String discountType,
                            Double discountValue, Double minOrderAmount, Double maxDiscountAmount,
                            LocalDateTime validFrom, LocalDateTime validTo, Integer usageLimit,
                            Integer usagePerCustomer, Boolean isActive, Boolean excludeSaleItems,
                            Boolean freeShipping, Boolean couponUsed) {
        LOGGER.fine("CouponRequestDto parameterized constructor called for code: " + couponCode);
        this.couponCode = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.usageLimit = usageLimit;
        this.usagePerCustomer = usagePerCustomer;
        this.isActive = isActive;
        this.excludeSaleItems = excludeSaleItems;
        this.freeShipping = freeShipping;
        this.couponUsed = couponUsed;
        LOGGER.fine("CouponRequestDto created with code: " + this.couponCode);
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getDescription() {
        LOGGER.finest("Getting description from request DTO");
        return description;
    }

    public void setDescription(String description) {
        LOGGER.fine("Setting description in request DTO to: " + description);
        this.description = description;
    }

    public String getDiscountType() {
        LOGGER.finest("Getting discountType from request DTO");
        return discountType;
    }

    public void setDiscountType(String discountType) {
        LOGGER.fine("Setting discountType in request DTO to: " + discountType);
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        LOGGER.finest("Getting discountValue from request DTO");
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        LOGGER.fine("Setting discountValue in request DTO to: " + discountValue);
        this.discountValue = discountValue;
    }

    public Double getMinOrderAmount() {
        LOGGER.finest("Getting minOrderAmount from request DTO");
        return minOrderAmount;
    }

    public void setMinOrderAmount(Double minOrderAmount) {
        LOGGER.fine("Setting minOrderAmount in request DTO to: " + minOrderAmount);
        this.minOrderAmount = minOrderAmount;
    }

    public Double getMaxDiscountAmount() {
        LOGGER.finest("Getting maxDiscountAmount from request DTO");
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        LOGGER.fine("Setting maxDiscountAmount in request DTO to: " + maxDiscountAmount);
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public LocalDateTime getValidFrom() {
        LOGGER.finest("Getting validFrom from request DTO");
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        LOGGER.fine("Setting validFrom in request DTO to: " + validFrom);
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        LOGGER.finest("Getting validTo from request DTO");
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        LOGGER.fine("Setting validTo in request DTO to: " + validTo);
        this.validTo = validTo;
    }

    public Integer getUsageLimit() {
        LOGGER.finest("Getting usageLimit from request DTO");
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        LOGGER.fine("Setting usageLimit in request DTO to: " + usageLimit);
        this.usageLimit = usageLimit;
    }

    public Integer getUsagePerCustomer() {
        LOGGER.finest("Getting usagePerCustomer from request DTO");
        return usagePerCustomer;
    }

    public void setUsagePerCustomer(Integer usagePerCustomer) {
        LOGGER.fine("Setting usagePerCustomer in request DTO to: " + usagePerCustomer);
        this.usagePerCustomer = usagePerCustomer;
    }

    public Boolean getIsActive() {
        LOGGER.finest("Getting isActive from request DTO");
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        LOGGER.fine("Setting isActive in request DTO to: " + isActive);
        this.isActive = isActive;
    }

    public Boolean getExcludeSaleItems() {
        LOGGER.finest("Getting excludeSaleItems from request DTO");
        return excludeSaleItems;
    }

    public void setExcludeSaleItems(Boolean excludeSaleItems) {
        LOGGER.fine("Setting excludeSaleItems in request DTO to: " + excludeSaleItems);
        this.excludeSaleItems = excludeSaleItems;
    }

    public Boolean getFreeShipping() {
        LOGGER.finest("Getting freeShipping from request DTO");
        return freeShipping;
    }

    public void setFreeShipping(Boolean freeShipping) {
        LOGGER.fine("Setting freeShipping in request DTO to: " + freeShipping);
        this.freeShipping = freeShipping;
    }

    public Boolean getCouponUsed() {
        return couponUsed;
    }

    public void setCouponUsed(Boolean couponUsed) {
        this.couponUsed = couponUsed;
    }
}
