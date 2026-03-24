package com.artezo.service;

import com.artezo.dto.request.CouponRequestDto;
import com.artezo.dto.response.CouponResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface CouponService {
    CouponResponseDto createCoupon(CouponRequestDto couponRequestDto);
    CouponResponseDto getCouponById(Long couponCode);
    CouponResponseDto getCouponByCode(String couponCode);
    List<CouponResponseDto> getAllCoupons();
    List<CouponResponseDto> getActiveCoupons();
    CouponResponseDto updateCoupon(Long couponId, CouponRequestDto couponRequestDto);
    void deleteCoupon(Long id);
    CouponResponseDto incrementUsedCount(Long couponId);
    boolean validateCoupon(String code, Double orderAmount);

    List<CouponResponseDto> getUserCoupons(Long userId);
}