package com.artezo.controller;

import com.artezo.dto.request.AddToCartRequest;
import com.artezo.dto.response.CartResponse;
import com.artezo.dto.response.CountResponse;
import com.artezo.exceptions.ApiResponse;
import com.artezo.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // GET /api/v1/cart?userId=1
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@RequestParam Long userId) {
        log.info("[CART] Fetching cart for userId={}", userId);
        try {
            CartResponse cart = cartService.getCart(userId);
            log.info("[CART] Cart fetched successfully for userId={}, totalItems={}", userId, cart.getTotalItems());
            return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", cart));
        } catch (RuntimeException ex) {
            log.warn("[CART] No active cart found for userId={} | reason={}", userId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No active cart found for userId: " + userId));
        }
    }

    // GET /api/v1/cart/session?sessionId=abc123
    @GetMapping("/session")
    public ResponseEntity<ApiResponse<CartResponse>> getCartBySession(@RequestParam String sessionId) {
        log.info("[CART] Fetching cart for sessionId={}", sessionId);
        try {
            CartResponse cart = cartService.getCartBySession(sessionId);
            log.info("[CART] Cart fetched for sessionId={}, totalItems={}", sessionId, cart.getTotalItems());
            return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", cart));
        } catch (RuntimeException ex) {
            log.warn("[CART] No active cart found for sessionId={} | reason={}", sessionId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No active cart found for session: " + sessionId));
        }
    }

    // POST /api/v1/cart/add
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@RequestBody AddToCartRequest request) {
        log.info("[CART] Add to cart request | userId={}, sessionId={}, productId={}, variantId={}",
                request.getUserId(), request.getSessionId(), request.getProductId(), request.getVariantId());
        try {
            CartResponse cart = cartService.addToCart(request);
            log.info("[CART] Item added to cart | userId={}, productId={}, sku={}, qty={}",
                    request.getUserId(), request.getProductId(), request.getSku(), request.getQuantity());
            return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", cart));
        } catch (RuntimeException ex) {
            log.error("[CART] Failed to add item to cart | userId={}, productId={} | reason={}",
                    request.getUserId(), request.getProductId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to add item to cart: " + ex.getMessage()));
        }
    }

    // PATCH /api/v1/cart/update-quantity?userId=1&itemId=5&quantity=3
    @PatchMapping("/update-quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @RequestParam Long userId,
            @RequestParam Long itemId,
            @RequestParam int quantity) {
        log.info("[CART] Update quantity | userId={}, itemId={}, newQuantity={}", userId, itemId, quantity);
        try {
            CartResponse cart = cartService.updateQuantity(userId, itemId, quantity);
            String msg = quantity <= 0 ? "Item removed from cart" : "Cart quantity updated successfully";
            log.info("[CART] Quantity updated | userId={}, itemId={}, quantity={}", userId, itemId, quantity);
            return ResponseEntity.ok(ApiResponse.success(msg, cart));
        } catch (RuntimeException ex) {
            log.warn("[CART] Quantity update failed | userId={}, itemId={} | reason={}", userId, itemId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Cart item not found with itemId: " + itemId));
        }
    }

    // DELETE /api/v1/cart/remove?userId=1&productId=10&variantId=VAR-GOLD
    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(required = false) String variantId) {
        log.info("[CART] Remove item | userId={}, productId={}, variantId={}", userId, productId, variantId);
        try {
            CartResponse cart = cartService.removeItem(userId, productId, variantId);
            log.info("[CART] Item removed | userId={}, productId={}, variantId={}", userId, productId, variantId);
            return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", cart));
        } catch (RuntimeException ex) {
            log.warn("[CART] Remove item failed | userId={}, productId={} | reason={}", userId, productId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to remove item: " + ex.getMessage()));
        }
    }

    // DELETE /api/v1/cart/clear?userId=1
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(@RequestParam Long userId) {
        log.info("[CART] Clear cart | userId={}", userId);
        try {
            cartService.clearCart(userId);
            log.info("[CART] Cart cleared successfully | userId={}", userId);
            return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
        } catch (RuntimeException ex) {
            log.warn("[CART] Clear cart failed | userId={} | reason={}", userId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No active cart found to clear for userId: " + userId));
        }
    }

    // POST /api/v1/cart/move-from-wishlist?userId=1&wishlistItemId=3
    @PostMapping("/move-from-wishlist")
    public ResponseEntity<ApiResponse<CartResponse>> moveFromWishlist(
            @RequestParam Long userId,
            @RequestParam Long wishlistItemId) {
        log.info("[CART] Move wishlist item to cart | userId={}, wishlistItemId={}", userId, wishlistItemId);
        try {
            CartResponse cart = cartService.moveWishlistItemToCart(userId, wishlistItemId);
            log.info("[CART] Wishlist item moved to cart | userId={}, wishlistItemId={}", userId, wishlistItemId);
            return ResponseEntity.ok(ApiResponse.success("Item moved from wishlist to cart successfully", cart));
        } catch (RuntimeException ex) {
            log.warn("[CART] Move from wishlist failed | userId={}, wishlistItemId={} | reason={}",
                    userId, wishlistItemId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Wishlist item not found with id: " + wishlistItemId));
        }
    }

    // GET /api/v1/cart/count?userId=1
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<CountResponse>> getCartCount(@RequestParam Long userId) {
        log.info("[CART] Fetching cart count | userId={}", userId);
        try {
            int count = cartService.getCartCount(userId);
            log.info("[CART] Cart count fetched | userId={}, count={}", userId, count);
            return ResponseEntity.ok(ApiResponse.success("Cart count fetched successfully",
                    CountResponse.builder().userId(userId).count(count).build()));
        } catch (RuntimeException ex) {
            log.warn("[CART] Cart count failed | userId={} | reason={}", userId, ex.getMessage());
            // return 0 instead of error — frontend shows 0 badge, not broken UI
            return ResponseEntity.ok(ApiResponse.success("No active cart found",
                    CountResponse.builder().userId(userId).count(0).build()));
        }
    }
}