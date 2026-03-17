package com.artezo.service.serviceImpl;

import com.artezo.dto.request.CouponRequestDto;
import com.artezo.dto.response.CouponResponseDto;
import com.artezo.entity.CouponEntity;
import com.artezo.repository.CouponRepository;
import com.artezo.service.CouponService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger LOGGER = Logger.getLogger(CouponServiceImpl.class.getName());

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        LOGGER.info("Initializing CouponServiceImpl");
        this.couponRepository = couponRepository;
        LOGGER.info("CouponServiceImpl initialized successfully");
    }

    @Override
    public CouponResponseDto createCoupon(CouponRequestDto requestDto) {
        LOGGER.info("Creating new coupon with code: " + requestDto.getCouponCode());

        LOGGER.fine("Checking if coupon code already exists: " + requestDto.getCouponCode());
        if (couponRepository.existsByCouponCode(requestDto.getCouponCode())) {
            LOGGER.warning("Coupon code already exists: " + requestDto.getCouponCode());
            throw new RuntimeException("Coupon with code " + requestDto.getCouponCode() + " already exists");
        }

        LOGGER.fine("Creating new CouponEntity from request DTO");
        CouponEntity coupon = new CouponEntity();
        coupon.setCouponCode(requestDto.getCouponCode());
        coupon.setDescription(requestDto.getDescription());
        coupon.setDiscountType(requestDto.getDiscountType());
        coupon.setDiscountValue(requestDto.getDiscountValue());
        coupon.setMinOrderAmount(requestDto.getMinOrderAmount());
        coupon.setMaxDiscountAmount(requestDto.getMaxDiscountAmount());
        coupon.setValidFrom(requestDto.getValidFrom());
        coupon.setValidTo(requestDto.getValidTo());
        coupon.setUsageLimit(requestDto.getUsageLimit());
        coupon.setUsagePerCustomer(requestDto.getUsagePerCustomer());
        coupon.setUsedCount(0);
        coupon.setIsActive(requestDto.getIsActive());
        coupon.setExcludeSaleItems(requestDto.getExcludeSaleItems());
        coupon.setFreeShipping(requestDto.getFreeShipping());
        coupon.setCreatedAt(LocalDateTime.now());

        LOGGER.fine("Saving coupon to database");
        CouponEntity savedCoupon = couponRepository.save(coupon);
        LOGGER.info("Coupon created successfully with couponId: " + savedCoupon.getCouponId() + ", code: " + savedCoupon.getCouponCode());

        CouponResponseDto response = convertToResponseDto(savedCoupon);
        LOGGER.fine("Converted saved coupon to response DTO");

        return response;
    }

    @Override
    public CouponResponseDto getCouponById(Long couponId) {
        LOGGER.info("Fetching coupon by couponId: " + couponId);

        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    LOGGER.warning("Coupon not found with couponId: " + couponId);
                    return new RuntimeException("Coupon not found with couponId: " + couponId);
                });

        LOGGER.info("Coupon found with couponId: " + couponId + ", couponCode: " + coupon.getCouponCode());
        LOGGER.fine("Converting coupon entity to response DTO");

        return convertToResponseDto(coupon);
    }

    @Override
    public CouponResponseDto getCouponByCode(String couponCode) {
        LOGGER.info("Fetching coupon by code: " + couponCode);

        CouponEntity coupon = couponRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> {
                    LOGGER.warning("Coupon not found with code: " + couponCode);
                    return new RuntimeException("Coupon not found with code: " + couponCode);
                });

        LOGGER.info("Coupon found with code: " + couponCode + ", couponId: " + coupon.getCouponCode());
        LOGGER.fine("Converting coupon entity to response DTO");

        return convertToResponseDto(coupon);
    }

    @Override
    public List<CouponResponseDto> getAllCoupons() {
        LOGGER.info("Fetching all coupons from database");

        List<CouponEntity> coupons = couponRepository.findAll();
        LOGGER.info("Found " + coupons.size() + " coupons in database");

        LOGGER.fine("Converting " + coupons.size() + " coupons to response DTOs");
        List<CouponResponseDto> responseList = coupons.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        LOGGER.fine("Successfully converted all coupons to response DTOs");

        return responseList;
    }

    @Override
    public List<CouponResponseDto> getActiveCoupons() {
        LOGGER.info("Fetching active coupons from database");

        LocalDateTime now = LocalDateTime.now();
        LOGGER.fine("Current date/time for active coupon check: " + now);

        List<CouponEntity> activeCoupons = couponRepository.findActiveCoupons(now);
        LOGGER.info("Found " + activeCoupons.size() + " active coupons");

        LOGGER.fine("Converting " + activeCoupons.size() + " active coupons to response DTOs");
        List<CouponResponseDto> responseList = activeCoupons.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        LOGGER.fine("Successfully converted active coupons to response DTOs");

        return responseList;
    }

    @Override
    public CouponResponseDto updateCoupon(Long couponId, CouponRequestDto requestDto) {
        LOGGER.info("Updating coupon with couponId: " + couponId);

        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    LOGGER.warning("Coupon not found with couponId: " + couponId);
                    return new RuntimeException("Coupon not found with couponId: " + couponId);
                });

        LOGGER.info("Found coupon to update with couponId: " + couponId + ", current code: " + coupon.getCouponId());

        if (requestDto.getCouponCode() != null) {
            LOGGER.fine("Updating code from " + coupon.getCouponCode() + " to: " + requestDto.getCouponCode());
            coupon.setCouponCode(requestDto.getCouponCode());
        }
        if (requestDto.getDescription() != null) {
            LOGGER.fine("Updating description to: " + requestDto.getDescription());
            coupon.setDescription(requestDto.getDescription());
        }
        if (requestDto.getDiscountType() != null) {
            LOGGER.fine("Updating discountType to: " + requestDto.getDiscountType());
            coupon.setDiscountType(requestDto.getDiscountType());
        }
        if (requestDto.getDiscountValue() != null) {
            LOGGER.fine("Updating discountValue to: " + requestDto.getDiscountValue());
            coupon.setDiscountValue(requestDto.getDiscountValue());
        }
        if (requestDto.getMinOrderAmount() != null) {
            LOGGER.fine("Updating minOrderAmount to: " + requestDto.getMinOrderAmount());
            coupon.setMinOrderAmount(requestDto.getMinOrderAmount());
        }
        if (requestDto.getMaxDiscountAmount() != null) {
            LOGGER.fine("Updating maxDiscountAmount to: " + requestDto.getMaxDiscountAmount());
            coupon.setMaxDiscountAmount(requestDto.getMaxDiscountAmount());
        }
        if (requestDto.getValidFrom() != null) {
            LOGGER.fine("Updating validFrom to: " + requestDto.getValidFrom());
            coupon.setValidFrom(requestDto.getValidFrom());
        }
        if (requestDto.getValidTo() != null) {
            LOGGER.fine("Updating validTo to: " + requestDto.getValidTo());
            coupon.setValidTo(requestDto.getValidTo());
        }
        if (requestDto.getUsageLimit() != null) {
            LOGGER.fine("Updating usageLimit to: " + requestDto.getUsageLimit());
            coupon.setUsageLimit(requestDto.getUsageLimit());
        }
        if (requestDto.getUsagePerCustomer() != null) {
            LOGGER.fine("Updating usagePerCustomer to: " + requestDto.getUsagePerCustomer());
            coupon.setUsagePerCustomer(requestDto.getUsagePerCustomer());
        }
        if (requestDto.getIsActive() != null) {
            LOGGER.fine("Updating isActive to: " + requestDto.getIsActive());
            coupon.setIsActive(requestDto.getIsActive());
        }
        if (requestDto.getExcludeSaleItems() != null) {
            LOGGER.fine("Updating excludeSaleItems to: " + requestDto.getExcludeSaleItems());
            coupon.setExcludeSaleItems(requestDto.getExcludeSaleItems());
        }
        if (requestDto.getFreeShipping() != null) {
            LOGGER.fine("Updating freeShipping to: " + requestDto.getFreeShipping());
            coupon.setFreeShipping(requestDto.getFreeShipping());
        }

        LOGGER.fine("Saving updated coupon to database");
        CouponEntity updatedCoupon = couponRepository.save(coupon);
        LOGGER.info("Coupon updated successfully with couponId: " + updatedCoupon.getCouponId());

        return convertToResponseDto(updatedCoupon);
    }

    @Override
    public void deleteCoupon(Long couponId) {
        LOGGER.info("Deleting coupon with couponId: " + couponId);

        if (!couponRepository.existsById(couponId)) {
            LOGGER.warning("Cannot delete - coupon not found with couponId: " + couponId);
            throw new RuntimeException("Coupon not found with couponId: " + couponId);
        }

        couponRepository.deleteById(couponId);
        LOGGER.info("Coupon deleted successfully with couponId: " + couponId);
    }

    @Override
    public CouponResponseDto incrementUsedCount(Long couponId) {
        LOGGER.info("Incrementing used count for coupon with couponId: " + couponId);

        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    LOGGER.warning("Coupon not found with couponId: " + couponId);
                    return new RuntimeException("Coupon not found with couponId: " + couponId);
                });

        int oldCount = coupon.getUsedCount();
        int newCount = oldCount + 1;
        LOGGER.info("Incrementing used count for coupon " + coupon.getCouponCode() + " from " + oldCount + " to " + newCount);

        coupon.setUsedCount(newCount);

        if (coupon.getUsageLimit() != null && newCount >= coupon.getUsageLimit()) {
            LOGGER.info("Coupon " + coupon.getCouponCode() + " has reached usage limit. Deactivating coupon.");
            coupon.setIsActive(false);
        }

        CouponEntity updatedCoupon = couponRepository.save(coupon);
        LOGGER.info("Used count incremented successfully for coupon couponId: " + couponId);

        return convertToResponseDto(updatedCoupon);
    }

    @Override
    public boolean validateCoupon(String code, Double orderAmount) {
        LOGGER.info("Validating coupon with code: " + code + " for order amount: " + orderAmount);

        CouponEntity coupon = couponRepository.findByCouponCode(code)
                .orElseThrow(() -> {
                    LOGGER.warning("Validation failed - coupon not found with code: " + code);
                    return new RuntimeException("Coupon not found with code: " + code);
                });

        LOGGER.info("Found coupon to validate: " + coupon.getCouponCode() + " (couponId: " + coupon.getCouponId() + ")");

        LocalDateTime now = LocalDateTime.now();

        if (!coupon.getIsActive()) {
            LOGGER.info("Validation failed - coupon is not active: " + code);
            return false;
        }

        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            LOGGER.info("Validation failed - coupon is outside validity period. Valid from: " +
                    coupon.getValidFrom() + " to: " + coupon.getValidTo() + ", current: " + now);
            return false;
        }

        if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            LOGGER.info("Validation failed - order amount " + orderAmount + " is less than minimum required: " +
                    coupon.getMinOrderAmount());
            return false;
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            LOGGER.info("Validation failed - coupon has reached usage limit. Used: " +
                    coupon.getUsedCount() + ", Limit: " + coupon.getUsageLimit());
            return false;
        }

        LOGGER.info("Coupon validation successful for code: " + code);
        return true;
    }

    private CouponResponseDto convertToResponseDto(CouponEntity coupon) {
        LOGGER.finest("Converting coupon entity to response DTO for coupon: " + coupon.getCouponCode());

        CouponResponseDto response = new CouponResponseDto(
                coupon.getCouponId(),
                coupon.getCouponCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getValidFrom(),
                coupon.getValidTo(),
                coupon.getUsageLimit(),
                coupon.getUsagePerCustomer(),
                coupon.getUsedCount(),
                coupon.getIsActive(),
                coupon.getExcludeSaleItems(),
                coupon.getFreeShipping(),
                coupon.getCreatedAt(),
                coupon.getCouponUsed()
        );

        LOGGER.finest("Successfully converted coupon " + coupon.getCouponCode() + " to response DTO");

        return response;
    }
}