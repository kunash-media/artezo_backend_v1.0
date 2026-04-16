package com.artezo.service.serviceImpl;

import com.artezo.dto.request.AddToWishlistRequest;
import com.artezo.dto.response.WishlistResponse;
import com.artezo.entity.UserEntity;
import com.artezo.entity.WishlistEntity;
import com.artezo.entity.WishlistItemEntity;
import com.artezo.repository.ProductRepository;
import com.artezo.repository.UserRepository;
import com.artezo.repository.WishlistItemRepository;
import com.artezo.repository.WishlistRepository;
import com.artezo.service.WishlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistServiceImpl implements WishlistService {

    private static final Logger logger = LoggerFactory.getLogger(WishlistServiceImpl.class);

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;

    public WishlistServiceImpl(WishlistRepository wishlistRepository, WishlistItemRepository wishlistItemRepository, UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(AddToWishlistRequest request) {
        String listName = request.getWishlistName() != null ? request.getWishlistName() : "My Wishlist";
        logger.info("[WISHLIST] Adding item | userId={}, productId={}, wishlistName={}",
                request.getUserId(), request.getProductId(), listName);

        WishlistEntity wishlist = wishlistRepository
                .findByUser_UserIdAndName(request.getUserId(), listName)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(request.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));
                    logger.info("[WISHLIST] Creating new wishlist '{}' for userId={}", listName, request.getUserId());
                    return wishlistRepository.save(
                            WishlistEntity.builder()
                                    .user(user)
                                    .name(listName)
                                    .build());
                });

        boolean alreadyAdded = wishlistItemRepository.existsByWishlist_IdAndProductIdAndVariantId(
                wishlist.getId(), request.getProductId(), request.getVariantId());

        if (!alreadyAdded) {
            WishlistItemEntity item = WishlistItemEntity.builder()
                    .wishlist(wishlist)
                    .productId(request.getProductId())
                    .variantId(request.getVariantId())
                    .sku(request.getSku())
                    .selectedColor(request.getSelectedColor())
                    .selectedSize(request.getSelectedSize())
                    .titleName(request.getProductName() != null ? request.getProductName() : request.getTitleName())                    .wishlistedPrice(request.getWishlistedPrice())
                    .customFieldsJson(request.getCustomFieldsJson())
                    .productImageUrl(calculateImageUrl(request.getProductId(), request.getVariantId()))
                    .build();
            wishlistItemRepository.save(item);
            logger.info("[WISHLIST] Item added | productId={}", request.getProductId());
        } else {
            logger.info("[WISHLIST] Item already in wishlist | productId={}", request.getProductId());
        }

        return buildWishlistResponse(wishlist);
    }

    private String calculateImageUrl(Long productPrimeId, String variantId) {
        if (productPrimeId == null) return null;

//        if (variantId != null && !variantId.trim().isEmpty()) {
//            return "/api/products/" + productPrimeId + "/variant/" + variantId + "/main";
//        }
        return "/api/products/" + productPrimeId + "/main";
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getAllWishlists(Long userId) {
        logger.info("[WISHLIST] Fetching all wishlists | userId={}", userId);
        return wishlistRepository.findByUser_UserId(userId).stream()
                .map(this::buildWishlistResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId, String wishlistName) {
        String name = wishlistName != null ? wishlistName : "My Wishlist";
        logger.info("[WISHLIST] Fetching wishlist | userId={}, name={}", userId, name);
        WishlistEntity wishlist = wishlistRepository.findByUser_UserIdAndName(userId, name)
                .orElseThrow(() -> new RuntimeException("Wishlist '" + name + "' not found for userId: " + userId));
        return buildWishlistResponse(wishlist);
    }

//    @Override
//    @Transactional
//    public void removeItem(Long userId, Long productId, String variantId) {
//        logger.info("[WISHLIST] Removing item | userId={}, productId={}, variantId={}", userId, productId, variantId);
//        wishlistRepository.findByUser_UserId(userId).forEach(wishlist ->
//                wishlistItemRepository.deleteByWishlist_IdAndProductIdAndVariantId(
//                        wishlist.getId(), productId, variantId));
//    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long productId, String variantId) {
        logger.info("[WISHLIST] Removing item | userId={}, productId={}, variantId={}",
                userId, productId, variantId);

        List<WishlistEntity> wishlists = wishlistRepository.findByUser_UserId(userId);

        if (wishlists.isEmpty()) {
            throw new RuntimeException(
                    "No wishlist found for userId=" + userId);
        }

        // Count how many rows were actually deleted across all wishlists for this user.
        // (Most users have exactly one wishlist, but the schema allows multiple.)
        int deletedCount = 0;
        for (WishlistEntity wishlist : wishlists) {

            // existsByWishlist_IdAndProductIdAndVariantId lets us check before delete
            // so we can count without relying on void deleteBy… return type.
            boolean exists = wishlistItemRepository
                    .existsByWishlist_IdAndProductIdAndVariantId(
                            wishlist.getId(), productId, variantId);

            if (exists) {
                wishlistItemRepository
                        .deleteByWishlist_IdAndProductIdAndVariantId(
                                wishlist.getId(), productId, variantId);
                deletedCount++;
            }
        }

        if (deletedCount == 0) {
            // Nothing matched — this is the silent-failure case that caused the bug.
            // Throwing here causes the controller to return 404 instead of 200,
            // which lets the frontend roll back the optimistic UI correctly.
            logger.warn("[WISHLIST] Item not found for removal | userId={}, productId={}, variantId={}",
                    userId, productId, variantId);
            throw new RuntimeException(
                    "Item not found in wishlist: productId=" + productId
                            + ", variantId=" + variantId);
        }

        logger.info("[WISHLIST] Item removed ({} row(s)) | userId={}, productId={}, variantId={}",
                deletedCount, userId, productId, variantId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId, String variantId) {
        return wishlistRepository.findByUser_UserId(userId).stream()
                .anyMatch(w -> wishlistItemRepository.existsByWishlist_IdAndProductIdAndVariantId(
                        w.getId(), productId, variantId));
    }

    @Override
    @Transactional
    public void clearWishlist(Long userId, String wishlistName) {
        String name = wishlistName != null ? wishlistName : "My Wishlist";
        logger.info("[WISHLIST] Clearing wishlist | userId={}, name={}", userId, name);
        WishlistEntity wishlist = wishlistRepository.findByUser_UserIdAndName(userId, name)
                .orElseThrow(() -> new RuntimeException("Wishlist '" + name + "' not found for userId: " + userId));
        wishlist.getItems().clear();
        wishlistRepository.save(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public int getWishlistCount(Long userId) {
        return wishlistItemRepository.countByUserId(userId);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

//    private WishlistResponse buildWishlistResponse(WishlistEntity wishlist) {
//        List<WishlistItemEntity> items = wishlistItemRepository.findByWishlist_Id(wishlist.getId());
//
//        List<WishlistResponse.WishlistItemResponse> itemResponses = items.stream()
//                .map(item -> WishlistResponse.WishlistItemResponse.builder()
//                        .itemId(item.getId())
//                        .productId(item.getProductId())
//                        .variantId(item.getVariantId())
//                        .sku(item.getSku())
//                        .selectedColor(item.getSelectedColor())
//                        .selectedSize(item.getSelectedSize())
//                        .titleName(item.getTitleName())
//                        .wishlistedPrice(item.getWishlistedPrice())
//                        .customFieldsJson(item.getCustomFieldsJson())
//                        .addedAt(item.getAddedAt())
//                        .build())
//                .collect(Collectors.toList());
//
//        return WishlistResponse.builder()
//                .wishlistId(wishlist.getId())
//                .userId(wishlist.getUser().getUserId())  // ← from UserEntity relation
//                .name(wishlist.getName())
//                .isPublic(wishlist.getIsPublic())
//                .items(itemResponses)
//                .totalItems(itemResponses.size())
//                .build();
//    }

    private WishlistResponse buildWishlistResponse(WishlistEntity wishlist) {

        List<WishlistItemEntity> items = wishlistItemRepository.findByWishlist_Id(wishlist.getId());


        List<WishlistResponse.WishlistItemResponse> itemResponses = items.stream()
                .map(item -> {
                    String imageUrl = item.getProductImageUrl();   // ← Use stored URL if available

                    // Fallback logic (only if stored URL is missing - rare after migration)
                    if (imageUrl == null && item.getProductId() != null) {
                        if (item.getVariantId() != null && !item.getVariantId().isEmpty()) {
                            imageUrl = "/api/products/" + item.getProductId()
                                    + "/variant/" + item.getVariantId() + "/main";
                        } else {
                            imageUrl = "/api/products/" + item.getProductId() + "/main";
                        }
                    }

                    return WishlistResponse.WishlistItemResponse.builder()
                            .itemId(item.getId())
                            .productId(item.getProductId())
                            .variantId(item.getVariantId())
                            .sku(item.getSku())
                            .selectedColor(item.getSelectedColor())
                            .selectedSize(item.getSelectedSize())
                            .titleName(item.getTitleName())
                            .wishlistedPrice(item.getWishlistedPrice())
                            .customFieldsJson(item.getCustomFieldsJson())
                            .addedAt(item.getAddedAt())
                            .productImageUrl(imageUrl)          // ← Always populated
                            .build();
                })
                .collect(Collectors.toList());


        return WishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .userId(wishlist.getUser().getUserId())
                .name(wishlist.getName())
                .isPublic(wishlist.getIsPublic())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .build();

    }
}