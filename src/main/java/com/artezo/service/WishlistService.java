package com.artezo.service;

import com.artezo.dto.request.AddToWishlistRequest;
import com.artezo.dto.response.WishlistResponse;

import java.util.List;

public interface WishlistService {

    WishlistResponse addToWishlist(AddToWishlistRequest request);

    List<WishlistResponse> getAllWishlists(Long userId);

    WishlistResponse getWishlist(Long userId, String wishlistName);

    void removeItem(Long userId, Long productId, String variantId);

    boolean isInWishlist(Long userId, Long productId, String variantId);

    void clearWishlist(Long userId, String wishlistName);

    int getWishlistCount(Long userId);
}