package com.artezo.service;

import com.artezo.dto.request.CouponRequestDto;
import com.artezo.dto.response.CouponResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface CouponService {

    CouponResponseDto createCoupon(CouponRequestDto couponRequestDto);
    CouponResponseDto getCouponById(Long couponId);
    CouponResponseDto getCouponByCode(String couponCode);
    List<CouponResponseDto> getAllCoupons();
    List<CouponResponseDto> getActiveCoupons();
    CouponResponseDto updateCoupon(Long couponId, CouponRequestDto couponRequestDto);
    void deleteCoupon(Long id);


    CouponResponseDto incrementUsedCount(Long couponId, Long userId);

    boolean validateCoupon(String code, Double orderAmount, Long userId, Long productId);

    List<CouponResponseDto> getUserCoupons(Long userId);
    List<CouponResponseDto> getProductCoupons(Long productId);

    List<CouponResponseDto> getCouponsByUserAndProduct(Long userId, Long productPrimeId);
}