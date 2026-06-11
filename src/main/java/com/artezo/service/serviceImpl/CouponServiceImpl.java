package com.artezo.service.serviceImpl;

import com.artezo.dto.request.CouponRequestDto;
import com.artezo.dto.response.CouponResponseDto;
import com.artezo.entity.*;
import com.artezo.repository.*;
import com.artezo.service.CouponService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger LOGGER = Logger.getLogger(CouponServiceImpl.class.getName());

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponUserUsageRepository usageRepository;
    private final ProductVariantRepository variantRepository;


    public CouponServiceImpl(CouponRepository couponRepository, UserRepository userRepository,
                             ProductRepository productRepository,
                             CouponUserUsageRepository usageRepository,
                             ProductVariantRepository variantRepository) {
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.usageRepository = usageRepository;
        this.variantRepository = variantRepository;
    }

    @Override
    @Transactional
    public CouponResponseDto createCoupon(CouponRequestDto requestDto) {
        LOGGER.info("Creating coupon: " + requestDto.getCouponCode());

        if (couponRepository.existsByCouponCode(requestDto.getCouponCode())) {
            throw new RuntimeException("Coupon already exists: " + requestDto.getCouponCode());
        }

        if (requestDto.getCouponCode() == null || requestDto.getCouponCode().trim().isEmpty()) {
            throw new RuntimeException("Coupon code cannot be empty");
        }
        if (requestDto.getDiscountValue() == null || requestDto.getDiscountValue() <= 0) {
            throw new RuntimeException("Discount value must be greater than 0");
        }
        if (requestDto.getValidFrom() == null || requestDto.getValidTo() == null) {
            throw new RuntimeException("Valid from and valid to dates are required");
        }
        if (requestDto.getValidFrom().isAfter(requestDto.getValidTo())) {
            throw new RuntimeException("Valid from date cannot be after valid to date");
        }

        CouponEntity coupon = new CouponEntity();
        coupon.setCouponCode(requestDto.getCouponCode().toUpperCase().trim());
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
        coupon.setIsActive(requestDto.getIsActive() != null ? requestDto.getIsActive() : true);
        coupon.setExcludeSaleItems(requestDto.getExcludeSaleItems() != null ? requestDto.getExcludeSaleItems() : false);
        coupon.setFreeShipping(requestDto.getFreeShipping() != null ? requestDto.getFreeShipping() : false);
        coupon.setCouponUsed(false);
        coupon.setCreatedAt(LocalDateTime.now());

        coupon.setCouponType(requestDto.getCouponType());
        coupon.setCategoryName(requestDto.getCategoryName());

        if (requestDto.getProductIds() != null && !requestDto.getProductIds().isEmpty()) {
            List<ProductEntity> products = productRepository.findAllById(requestDto.getProductIds());
            coupon.setProducts(products);
        }

        if (requestDto.getUserIds() != null && !requestDto.getUserIds().isEmpty()) {
            List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());
            coupon.setAllowedUsers(users);
        }

        // ADD after the allowedUsers block in createCoupon()
        if (requestDto.getVariantIds() != null && !requestDto.getVariantIds().isEmpty()) {
            List<ProductVariantEntity> variants = variantRepository.findAllById(requestDto.getVariantIds());
            coupon.setAllowedVariants(variants);
        }

        CouponEntity saved = couponRepository.save(coupon);
        LOGGER.info("Coupon created: " + saved.getCouponId());
        return convertToResponseDto(saved,null);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponById(Long couponId) {
        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + couponId));
        return convertToResponseDto(coupon, null); // ← admin fetch, no user context
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponByCode(String couponCode) {
        CouponEntity coupon = couponRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + couponCode));
        return convertToResponseDto(coupon, null); // ← used by validate endpoint, no user context needed
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(coupon -> convertToResponseDto(coupon, null)) // ← lambda, not method ref
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getActiveCoupons() {
        return couponRepository.findActiveCoupons(LocalDateTime.now()).stream()
                .map(coupon -> convertToResponseDto(coupon, null)) // ← lambda, not method ref
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponResponseDto updateCoupon(Long couponId, CouponRequestDto requestDto) {
        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + couponId));

        if (requestDto.getCouponCode() != null) coupon.setCouponCode(requestDto.getCouponCode());
        if (requestDto.getDescription() != null) coupon.setDescription(requestDto.getDescription());
        if (requestDto.getDiscountType() != null) coupon.setDiscountType(requestDto.getDiscountType());
        if (requestDto.getDiscountValue() != null) coupon.setDiscountValue(requestDto.getDiscountValue());
        if (requestDto.getMinOrderAmount() != null) coupon.setMinOrderAmount(requestDto.getMinOrderAmount());
        if (requestDto.getMaxDiscountAmount() != null) coupon.setMaxDiscountAmount(requestDto.getMaxDiscountAmount());
        if (requestDto.getValidFrom() != null) coupon.setValidFrom(requestDto.getValidFrom());
        if (requestDto.getValidTo() != null) coupon.setValidTo(requestDto.getValidTo());
        if (requestDto.getUsageLimit() != null) coupon.setUsageLimit(requestDto.getUsageLimit());
        if (requestDto.getUsagePerCustomer() != null) coupon.setUsagePerCustomer(requestDto.getUsagePerCustomer());
        if (requestDto.getIsActive() != null) coupon.setIsActive(requestDto.getIsActive());
        if (requestDto.getExcludeSaleItems() != null) coupon.setExcludeSaleItems(requestDto.getExcludeSaleItems());
        if (requestDto.getFreeShipping() != null) coupon.setFreeShipping(requestDto.getFreeShipping());
        if (requestDto.getCouponType() != null) coupon.setCouponType(requestDto.getCouponType());
        if (requestDto.getCategoryName() != null) coupon.setCategoryName(requestDto.getCategoryName());

        if (requestDto.getProductIds() != null) {
            List<ProductEntity> products = productRepository.findAllById(requestDto.getProductIds());
            coupon.setProducts(products);
        }

        if (requestDto.getUserIds() != null) {
            List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());
            coupon.setAllowedUsers(users);
        }

        // ADD after the allowedUsers block in updateCoupon()
        if (requestDto.getVariantIds() != null) {
            List<ProductVariantEntity> variants = variantRepository.findAllById(requestDto.getVariantIds());
            coupon.setAllowedVariants(variants);
        }

        return convertToResponseDto(couponRepository.save(coupon),null);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new RuntimeException("Coupon not found: " + couponId);
        }
        couponRepository.deleteById(couponId);
    }

    @Override
    @Transactional
    public CouponResponseDto incrementUsedCount(Long couponId, Long userId) {
        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + couponId));

        int newCount = coupon.getUsedCount() + 1;
        coupon.setUsedCount(newCount);

        if (coupon.getUsageLimit() != null && newCount >= coupon.getUsageLimit()) {
            coupon.setIsActive(false);
        }


        couponRepository.save(coupon);

        if (userId != null) {
            CouponUserUsage usage = usageRepository
                    .findByCouponIdAndUserId(couponId, userId)
                    .orElse(new CouponUserUsage(couponId, userId));
            usage.setUsedCount(usage.getUsedCount() + 1);
            usageRepository.save(usage);
        }

        return convertToResponseDto(coupon,null);
    }

    @Override
    @Transactional
    public boolean validateCoupon(String code, Double orderAmount, Long userId, Long productId) {
        CouponEntity coupon = couponRepository.findByCouponCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + code));

        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            LOGGER.info("Coupon inactive: " + code);
            return false;
        }

        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            LOGGER.info("Coupon expired or not started: " + code);
            return false;
        }

        if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            LOGGER.info("Order amount too low for coupon: " + code);
            return false;
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            LOGGER.info("Coupon usage limit reached: " + code);
            return false;
        }

        if (userId != null && !coupon.getAllowedUsers().isEmpty()) {
            boolean allowed = coupon.getAllowedUsers().stream()
                    .anyMatch(u -> u.getUserId().equals(userId));
            if (!allowed) {
                LOGGER.info("User not allowed for coupon: " + code);
                return false;
            }
        }

        if (userId != null && coupon.getUsagePerCustomer() != null) {
            CouponUserUsage usage = usageRepository
                    .findByCouponIdAndUserId(coupon.getCouponId(), userId)
                    .orElse(null);
            if (usage != null && usage.getUsedCount() >= coupon.getUsagePerCustomer()) {
                LOGGER.info("User exceeded per-customer limit for: " + code);
                return false;
            }
        }

        if (productId != null && !coupon.getProducts().isEmpty()) {
            boolean productAllowed = coupon.getProducts().stream()
                    .anyMatch(p -> p.getProductPrimeId().equals(productId));
            if (!productAllowed) {
                LOGGER.info("Product not applicable for coupon: " + code);
                return false;
            }
        }

        LOGGER.info("Coupon valid: " + code);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getUserCoupons(Long userId) {
        return couponRepository.findActiveCouponsForUser(userId).stream()
                .filter(coupon -> {
                    if (coupon.getUsagePerCustomer() == null) return true;
                    return usageRepository
                            .findByCouponIdAndUserId(coupon.getCouponId(), userId)
                            .map(u -> u.getUsedCount() < coupon.getUsagePerCustomer())
                            .orElse(true); // never used by this user = eligible
                })
                .map(coupon -> convertToResponseDto(coupon, userId)) // ← pass userId
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public List<CouponResponseDto> getProductCoupons(Long productId) {
        return couponRepository.findCouponsByProduct(productId).stream()
                .map(coupon -> convertToResponseDto(coupon,null))   // Explicit call instead of method reference
                .collect(Collectors.toList());
    }



    private CouponResponseDto convertToResponseDto(CouponEntity coupon, Long userId) {
        CouponResponseDto response = new CouponResponseDto();
        response.setCouponId(coupon.getCouponId());
        response.setCouponCode(coupon.getCouponCode());
        response.setDescription(coupon.getDescription());
        response.setDiscountType(coupon.getDiscountType());
        response.setDiscountValue(coupon.getDiscountValue());
        response.setMinOrderAmount(coupon.getMinOrderAmount());
        response.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        response.setValidFrom(coupon.getValidFrom());
        response.setValidTo(coupon.getValidTo());
        response.setUsageLimit(coupon.getUsageLimit());
        response.setUsagePerCustomer(coupon.getUsagePerCustomer());
        response.setUsedCount(coupon.getUsedCount());
        response.setIsActive(coupon.getIsActive());
        response.setExcludeSaleItems(coupon.getExcludeSaleItems());
        response.setFreeShipping(coupon.getFreeShipping());
        response.setCreatedAt(coupon.getCreatedAt());
        response.setCouponType(coupon.getCouponType());
        response.setCategoryName(coupon.getCategoryName());

        // ── couponUsed: reflects THIS user's state, not a global flag ────────────
        boolean globallyExhausted = coupon.getUsageLimit() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit();

        boolean userExhausted = false;
        if (userId != null && coupon.getUsagePerCustomer() != null) {
            userExhausted = usageRepository
                    .findByCouponIdAndUserId(coupon.getCouponId(), userId)
                    .map(u -> u.getUsedCount() >= coupon.getUsagePerCustomer())
                    .orElse(false);
        }
        response.setCouponUsed(globallyExhausted || userExhausted);
        // ─────────────────────────────────────────────────────────────────────────

        if (coupon.getProducts() != null) {
            response.setProductIds(
                    coupon.getProducts().stream()
                            .map(ProductEntity::getProductPrimeId)
                            .collect(Collectors.toList())
            );
        }
        if (coupon.getAllowedUsers() != null) {
            response.setUserIds(
                    coupon.getAllowedUsers().stream()
                            .map(UserEntity::getUserId)
                            .collect(Collectors.toList())
            );
        }
        if (coupon.getAllowedVariants() != null) {
            response.setVariantIds(
                    coupon.getAllowedVariants().stream()
                            .map(ProductVariantEntity::getId)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }


    // CouponServiceImpl.java
// REPLACE the existing getCouponsByUserAndProduct method

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getCouponsByUserAndProduct(Long userId, Long productPrimeId) {
        List<CouponEntity> coupons = couponRepository.findActiveByProductPrimeId(productPrimeId);

        return coupons.stream()
                .filter(coupon -> {
                    // filter out coupons this user has already exhausted
                    if (coupon.getUsagePerCustomer() == null) return true;
                    return usageRepository
                            .findByCouponIdAndUserId(coupon.getCouponId(), userId)
                            .map(u -> u.getUsedCount() < coupon.getUsagePerCustomer())
                            .orElse(true);
                })
                .map(c -> {
                    Long cid = c.getCouponId();

                    List<UserEntity> users = couponRepository.findWithAllowedUsers(cid)
                            .map(CouponEntity::getAllowedUsers)
                            .orElse(Collections.emptyList());

                    List<ProductVariantEntity> variants = couponRepository.findWithAllowedVariants(cid)
                            .map(CouponEntity::getAllowedVariants)
                            .orElse(Collections.emptyList());

                    return mapToDto(c, userId, users, variants);
                })
                .collect(Collectors.toList());
    }

    // CouponServiceImpl.java — add this method

    @Override
    @Transactional(readOnly = true)
    public String validateCouponWithReason(String code, Double orderAmount, Long userId, Long productId) {
        CouponEntity coupon = couponRepository.findByCouponCode(code).orElse(null);
        if (coupon == null)                    return "INVALID_CODE";

        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(coupon.getIsActive()))         return "INACTIVE";
        if (now.isBefore(coupon.getValidFrom()))                return "NOT_STARTED";
        if (now.isAfter(coupon.getValidTo()))                   return "EXPIRED";
        if (coupon.getMinOrderAmount() != null
                && orderAmount < coupon.getMinOrderAmount())    return "MIN_ORDER_NOT_MET";
        if (coupon.getUsageLimit() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit()) return "GLOBALLY_EXHAUSTED";

        if (userId != null) {
            // user whitelist check
            if (!coupon.getAllowedUsers().isEmpty()) {
                boolean allowed = coupon.getAllowedUsers().stream()
                        .anyMatch(u -> u.getUserId().equals(userId));
                if (!allowed) return "USER_NOT_ELIGIBLE";
            }

            // per-customer limit check
            if (coupon.getUsagePerCustomer() != null) {
                boolean exhausted = usageRepository
                        .findByCouponIdAndUserId(coupon.getCouponId(), userId)
                        .map(u -> u.getUsedCount() >= coupon.getUsagePerCustomer())
                        .orElse(false);
                if (exhausted) return "ALREADY_USED";
            }
        }

        if (productId != null && !coupon.getProducts().isEmpty()) {
            boolean productAllowed = coupon.getProducts().stream()
                    .anyMatch(p -> p.getProductPrimeId().equals(productId));
            if (!productAllowed) return "PRODUCT_NOT_ELIGIBLE";
        }

        return "VALID";
    }



    private CouponResponseDto mapToDto(CouponEntity c, Long userId,
                                       List<UserEntity> users,
                                       List<ProductVariantEntity> variants) {
        CouponResponseDto dto = new CouponResponseDto();

        dto.setCouponId(c.getCouponId());
        dto.setCouponCode(c.getCouponCode());
        dto.setDescription(c.getDescription());
        dto.setDiscountType(c.getDiscountType());
        dto.setDiscountValue(c.getDiscountValue());
        dto.setMinOrderAmount(c.getMinOrderAmount());
        dto.setMaxDiscountAmount(c.getMaxDiscountAmount());
        dto.setValidFrom(c.getValidFrom());
        dto.setValidTo(c.getValidTo());
        dto.setUsageLimit(c.getUsageLimit());
        dto.setUsagePerCustomer(c.getUsagePerCustomer());
        dto.setUsedCount(c.getUsedCount());
        dto.setIsActive(c.getIsActive());
        dto.setExcludeSaleItems(c.getExcludeSaleItems());
        dto.setFreeShipping(c.getFreeShipping());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setCouponType(c.getCouponType());
        dto.setCategoryName(c.getCategoryName());

        // ── couponUsed: derived from CouponUserUsage, not the entity flag ────────
        boolean globallyExhausted = c.getUsageLimit() != null
                && c.getUsedCount() >= c.getUsageLimit();

        boolean userExhausted = false;
        if (userId != null && c.getUsagePerCustomer() != null) {
            userExhausted = usageRepository
                    .findByCouponIdAndUserId(c.getCouponId(), userId)
                    .map(u -> u.getUsedCount() >= c.getUsagePerCustomer())
                    .orElse(false);
        }
        dto.setCouponUsed(globallyExhausted || userExhausted);
        // ─────────────────────────────────────────────────────────────────────────

        dto.setProductIds(
                c.getProducts().stream()
                        .map(ProductEntity::getProductPrimeId)
                        .collect(Collectors.toList())
        );
        dto.setVariantIds(
                variants.stream()
                        .map(ProductVariantEntity::getId)
                        .collect(Collectors.toList())
        );
        dto.setUserIds(
                users.stream()
                        .map(UserEntity::getUserId)
                        .collect(Collectors.toList())
        );

        return dto;
    }
}