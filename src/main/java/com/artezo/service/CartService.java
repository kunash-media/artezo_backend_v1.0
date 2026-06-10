package com.artezo.service;

import com.artezo.dto.request.AddToCartRequest;
import com.artezo.dto.request.RemoveCartItemsRequest;
import com.artezo.dto.response.CartResponse;

import java.util.List;

public interface CartService {

    CartResponse addToCart(AddToCartRequest request);

    CartResponse getCart(Long userId);

    CartResponse getCartBySession(String sessionId);

    CartResponse updateQuantity(Long userId, Long itemId, int quantity);

    CartResponse removeItem(Long userId, Long productId, String variantId);

    void clearCart(Long userId);

    // move wishlist item directly to cart
    CartResponse moveWishlistItemToCart(Long userId, Long wishlistItemId);


    int getCartCount(Long userId);

    CartResponse removeItems(Long userId, List<RemoveCartItemsRequest.CartItemIdentifier> items);
}