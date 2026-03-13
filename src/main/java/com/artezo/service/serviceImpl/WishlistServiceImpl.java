package com.artezo.service.serviceImpl;

import com.artezo.dto.request.AddToWishlistRequest;
import com.artezo.dto.response.WishlistResponse;
import com.artezo.entity.WishlistEntity;
import com.artezo.entity.WishlistItemEntity;
import com.artezo.repository.WishlistItemRepository;
import com.artezo.repository.WishlistRepository;
import com.artezo.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;

    @Override
    @Transactional
    public WishlistResponse addToWishlist(AddToWishlistRequest request) {
        String listName = request.getWishlistName() != null ? request.getWishlistName() : "My Wishlist";

        // get or create wishlist by name
        WishlistEntity wishlist = wishlistRepository
                .findByUserIdAndName(request.getUserId(), listName)
                .orElseGet(() -> wishlistRepository.save(
                        WishlistEntity.builder()
                                .userId(request.getUserId())
                                .name(listName)
                                .build()));

        // skip if already in wishlist
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
                    .titleName(request.getTitleName())
                    .wishlistedPrice(request.getWishlistedPrice())
                    .customFieldsJson(request.getCustomFieldsJson())
                    .build();
            wishlistItemRepository.save(item);
        }

        return buildWishlistResponse(wishlist);
    }

    @Override
    public List<WishlistResponse> getAllWishlists(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(this::buildWishlistResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WishlistResponse getWishlist(Long userId, String wishlistName) {
        String name = wishlistName != null ? wishlistName : "My Wishlist";
        WishlistEntity wishlist = wishlistRepository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new RuntimeException("Wishlist not found: " + name));
        return buildWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long productId, String variantId) {
        wishlistRepository.findByUserId(userId).forEach(wishlist ->
                wishlistItemRepository.deleteByWishlist_IdAndProductIdAndVariantId(
                        wishlist.getId(), productId, variantId));
    }

    @Override
    public boolean isInWishlist(Long userId, Long productId, String variantId) {
        return wishlistRepository.findByUserId(userId).stream()
                .anyMatch(w -> wishlistItemRepository.existsByWishlist_IdAndProductIdAndVariantId(
                        w.getId(), productId, variantId));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private WishlistResponse buildWishlistResponse(WishlistEntity wishlist) {
        List<WishlistItemEntity> items = wishlistItemRepository.findByWishlist_Id(wishlist.getId());

        List<WishlistResponse.WishlistItemResponse> itemResponses = items.stream()
                .map(item -> WishlistResponse.WishlistItemResponse.builder()
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
                        .build())
                .collect(Collectors.toList());

        return WishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .userId(wishlist.getUserId())
                .name(wishlist.getName())
                .isPublic(wishlist.getIsPublic())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .build();
    }


    @Override
    @Transactional
    public void clearWishlist(Long userId, String wishlistName) {
        String name = wishlistName != null ? wishlistName : "My Wishlist";
        WishlistEntity wishlist = wishlistRepository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new RuntimeException("Wishlist '" + name + "' not found for userId: " + userId));
        wishlist.getItems().clear();
        wishlistRepository.save(wishlist);
    }

    @Override
    public int getWishlistCount(Long userId) {
        return wishlistItemRepository.countByWishlistUserId(userId);
        // counts across ALL wishlists of the user
    }

}