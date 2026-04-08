package com.artezo.controller;

import com.artezo.dto.request.CouponRequestDto;
import com.artezo.dto.response.CouponResponseDto;
import com.artezo.service.CouponService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private static final Logger LOGGER = Logger.getLogger(CouponController.class.getName());

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        LOGGER.info("Initializing CouponController");
        this.couponService = couponService;
        LOGGER.info("CouponController initialized successfully");
    }

    @PostMapping("/create-coupon")
    public ResponseEntity<CouponResponseDto> createCoupon(@RequestBody CouponRequestDto couponRequestDto) {
        LOGGER.info("Received request to create new coupon with couponCode: " +
                (couponRequestDto.getCouponCode() != null ? couponRequestDto.getCouponCode() : "null"));

        LOGGER.fine("Create coupon request details - couponCode: " + couponRequestDto.getCouponCode() +
                ", Type: " + couponRequestDto.getDiscountType() +
                ", Value: " + couponRequestDto.getDiscountValue());

        CouponResponseDto createdCoupon = couponService.createCoupon(couponRequestDto);

        LOGGER.info("Coupon created successfully with couponId: " + createdCoupon.getCouponId() +
                ", couponCode: " + createdCoupon.getCouponCode());

        return new ResponseEntity<>(createdCoupon, HttpStatus.CREATED);
    }

    @GetMapping("/get-all-coupons")
    public ResponseEntity<List<CouponResponseDto>> getAllCoupons() {
        LOGGER.info("Received request to fetch all coupons");

        List<CouponResponseDto> coupons = couponService.getAllCoupons();

        LOGGER.info("Returning " + coupons.size() + " coupons in response");

        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/get-by-userId/{userId}")
    public ResponseEntity<List<CouponResponseDto>> getUserCoupons(@PathVariable("userId") Long userId) {

        LOGGER.info("Received request to fetch all coupons by userId:{}" + userId);

        List<CouponResponseDto> userCoupons = couponService.getUserCoupons(userId);

        return ResponseEntity.ok(userCoupons);

    }

    @GetMapping("/get-active-coupons/active")
    public ResponseEntity<List<CouponResponseDto>> getActiveCoupons() {
        LOGGER.info("Received request to fetch active coupons");

        List<CouponResponseDto> activeCoupons = couponService.getActiveCoupons();

        LOGGER.info("Returning " + activeCoupons.size() + " active coupons in response");

        return ResponseEntity.ok(activeCoupons);
    }

    @GetMapping("/get-coupon-by-couponId/{couponId}")
    public ResponseEntity<CouponResponseDto> getCouponById(@PathVariable Long couponId) {
        LOGGER.info("Received request to fetch coupon by couponId: " + couponId);

        CouponResponseDto coupon = couponService.getCouponById(couponId);

        LOGGER.info("Returning coupon with couponId: " + couponId + ", couponCode: " + coupon.getCouponCode());

        return ResponseEntity.ok(coupon);
    }

    @GetMapping("/get-coupon-by-couponCode/couponCode/{couponCode}")
    public ResponseEntity<CouponResponseDto> getCouponByCode(@PathVariable String couponCode) {
        LOGGER.info("Received request to fetch coupon by couponCode: " + couponCode);

        CouponResponseDto coupon = couponService.getCouponByCode(couponCode);

        LOGGER.info("Returning coupon with couponCode: " + couponCode + ", couponId: " + coupon.getCouponId());

        return ResponseEntity.ok(coupon);
    }


    @PatchMapping("/patch-coupon/{couponId}")
    public ResponseEntity<CouponResponseDto> patchCoupon(
            @PathVariable Long couponId,
            @RequestBody Map<String, Object> updates) {
        LOGGER.info("Received request to patch coupon with couponId: " + couponId);
        LOGGER.fine("Patch fields received: " + updates.keySet());

        CouponRequestDto patchDto = new CouponRequestDto();

        // Handle description
        if (updates.containsKey("description")) {
            String description = (String) updates.get("description");
            if (description == null || description.trim().isEmpty()) {
                throw new RuntimeException("Description cannot be empty");
            }
            patchDto.setDescription(description);
            LOGGER.fine("Updating description for couponId: " + couponId);
        }

        // Handle discount value and max discount amount
        if (updates.containsKey("discountValue")) {
            patchDto.setDiscountValue(((Number) updates.get("discountValue")).doubleValue());
            LOGGER.fine("Updating discount value for couponId: " + couponId);
        }

        if (updates.containsKey("maxDiscountAmount")) {
            patchDto.setMaxDiscountAmount(((Number) updates.get("maxDiscountAmount")).doubleValue());
            LOGGER.fine("Updating max discount amount for couponId: " + couponId);
        }

        // Handle validity dates
        if (updates.containsKey("validFrom")) {
            patchDto.setValidFrom(java.time.LocalDateTime.parse((String) updates.get("validFrom")));
            LOGGER.fine("Updating validFrom for couponId: " + couponId);
        }

        if (updates.containsKey("validTo")) {
            patchDto.setValidTo(java.time.LocalDateTime.parse((String) updates.get("validTo")));
            LOGGER.fine("Updating validTo for couponId: " + couponId);
        }

        // Handle status
        if (updates.containsKey("isActive")) {
            patchDto.setIsActive((Boolean) updates.get("isActive"));
            LOGGER.fine("Updating isActive status for couponId: " + couponId + " to: " + updates.get("isActive"));
        }

        // Handle usage limit
        if (updates.containsKey("usageLimit")) {
            Integer usageLimit = (Integer) updates.get("usageLimit");
            if (usageLimit <= 0) {
                throw new RuntimeException("Usage limit must be greater than 0");
            }
            patchDto.setUsageLimit(usageLimit);
            LOGGER.fine("Updating usage limit for couponId: " + couponId + " to: " + usageLimit);
        }

        // Handle minimum order amount
        if (updates.containsKey("minOrderAmount")) {
            Double minOrderAmount = ((Number) updates.get("minOrderAmount")).doubleValue();
            if (minOrderAmount < 0) {
                throw new RuntimeException("Minimum order amount cannot be negative");
            }
            patchDto.setMinOrderAmount(minOrderAmount);
            LOGGER.fine("Updating min order amount for couponId: " + couponId + " to: " + minOrderAmount);
        }

        // Handle boolean flags
        if (updates.containsKey("excludeSaleItems")) {
            patchDto.setExcludeSaleItems((Boolean) updates.get("excludeSaleItems"));
            LOGGER.fine("Updating excludeSaleItems for couponId: " + couponId);
        }

        if (updates.containsKey("freeShipping")) {
            patchDto.setFreeShipping((Boolean) updates.get("freeShipping"));
            LOGGER.fine("Updating freeShipping for couponId: " + couponId);
        }

        // Handle coupon code (if you want to allow updating it)
        if (updates.containsKey("couponCode")) {
            String couponCode = (String) updates.get("couponCode");
            if (couponCode == null || couponCode.trim().isEmpty()) {
                throw new RuntimeException("Coupon code cannot be empty");
            }
            patchDto.setCouponCode(couponCode);
            LOGGER.fine("Updating coupon code for couponId: " + couponId + " to: " + couponCode);
        }

        // Handle discount type (if you want to allow updating it)
        if (updates.containsKey("discountType")) {
            patchDto.setDiscountType((String) updates.get("discountType"));
            LOGGER.fine("Updating discount type for couponId: " + couponId);
        }

        CouponResponseDto updatedCoupon = couponService.updateCoupon(couponId, patchDto);
        LOGGER.info("Patch update successful for couponId: " + couponId);

        return ResponseEntity.ok(updatedCoupon);
    }

    @PutMapping("/update-coupon/{couponId}")
    public ResponseEntity<CouponResponseDto> updateCoupon(@PathVariable Long couponId,
                                                          @RequestBody CouponRequestDto couponRequestDto) {
        LOGGER.info("Received request to update coupon with couponId: " + couponId);

        LOGGER.fine("Update request details for coupon couponId " + couponId +
                " - New couponCode: " + couponRequestDto.getCouponCode() +
                ", New description: " + couponRequestDto.getDescription());

        CouponResponseDto updatedCoupon = couponService.updateCoupon(couponId, couponRequestDto);

        LOGGER.info("Coupon updated successfully with couponId: " + couponId);

        return ResponseEntity.ok(updatedCoupon);
    }

    @DeleteMapping("/delete-coupon/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) {
        LOGGER.info("Received request to delete coupon with couponId: " + couponId);

        couponService.deleteCoupon(couponId);

        LOGGER.info("Coupon deleted successfully with couponId: " + couponId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/increment-used-count/{couponId}/increment-usage")
    public ResponseEntity<CouponResponseDto> incrementUsedCount(@PathVariable Long couponId) {
        LOGGER.info("Received request to increment usage count for coupon couponId: " + couponId);

        CouponResponseDto updatedCoupon = couponService.incrementUsedCount(couponId);

        LOGGER.info("Usage count incremented successfully for coupon couponId: " + couponId +
                ", New count: " + updatedCoupon.getUsedCount());

        return ResponseEntity.ok(updatedCoupon);
    }

    @GetMapping("/validate-coupon/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestParam String couponCode,
                                                              @RequestParam Double orderAmount) {
        LOGGER.info("Received request to validate coupon - couponCode: " + couponCode + ", Order Amount: " + orderAmount);

        boolean isValid = couponService.validateCoupon(couponCode, orderAmount);

        LOGGER.info("Validation result for coupon " + couponCode + ": " + (isValid ? "VALID" : "INVALID"));

        Map<String, Object> response = new HashMap<>();
        response.put("couponCode", couponCode);
        response.put("valid", isValid);
        response.put("orderAmount", orderAmount);
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        if (isValid) {
            LOGGER.fine("Coupon is valid, fetching additional details for response");
            CouponResponseDto coupon = couponService.getCouponByCode(couponCode);
            response.put("coupon", coupon);
            response.put("message", "Coupon is valid and can be applied");
        } else {
            response.put("message", "Coupon is not valid for this order");
        }

        LOGGER.fine("Returning validation response for coupon: " + couponCode);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        LOGGER.warning("Exception occurred: " + ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.badRequest().body(errorResponse);
    }
}