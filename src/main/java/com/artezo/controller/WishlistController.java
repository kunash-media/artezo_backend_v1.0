package com.artezo.controller;

import com.artezo.dto.request.AddToWishlistRequest;
import com.artezo.dto.response.CountResponse;
import com.artezo.dto.response.WishlistResponse;
import com.artezo.exceptions.ApiResponse;
import com.artezo.service.WishlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private static final Logger log = LoggerFactory.getLogger(WishlistController.class);

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    // GET /api/v1/wishlist?userId=1
    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getAllWishlists(@RequestParam Long userId) {
        log.info("[WISHLIST] Fetching all wishlists | userId={}", userId);
        try {
            List<WishlistResponse> wishlists = wishlistService.getAllWishlists(userId);
            log.info("[WISHLIST] Wishlists fetched | userId={}, count={}", userId, wishlists.size());
            return ResponseEntity.ok(ApiResponse.success("Wishlists fetched successfully", wishlists));
        } catch (RuntimeException ex) {
            log.warn("[WISHLIST] Failed to fetch wishlists | userId={} | reason={}", userId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No wishlists found for userId: " + userId));
        }
    }

    // GET /api/v1/wishlist/items?userId=1&wishlistName=My Wishlist
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(
            @RequestParam Long userId,
            @RequestParam(required = false) String wishlistName) {
        String resolvedName = wishlistName != null ? wishlistName : "My Wishlist";
        log.info("[WISHLIST] Fetching wishlist | userId={}, wishlistName={}", userId, resolvedName);
        try {
            WishlistResponse wishlist = wishlistService.getWishlist(userId, wishlistName);
            log.info("[WISHLIST] Wishlist fetched | userId={}, wishlistName={}, totalItems={}",
                    userId, resolvedName, wishlist.getTotalItems());
            return ResponseEntity.ok(ApiResponse.success("Wishlist fetched successfully", wishlist));
        } catch (RuntimeException ex) {
            log.warn("[WISHLIST] Wishlist not found | userId={}, wishlistName={} | reason={}",
                    userId, resolvedName, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Wishlist '" + resolvedName + "' not found for userId: " + userId));
        }
    }

    // POST /api/v1/wishlist/add
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(@RequestBody AddToWishlistRequest request) {
        log.info("[WISHLIST] Add to wishlist | userId={}, productId={}, variantId={}, wishlistName={}",
                request.getUserId(), request.getProductId(), request.getVariantId(), request.getWishlistName());
        try {
            WishlistResponse wishlist = wishlistService.addToWishlist(request);
            log.info("[WISHLIST] Item added | userId={}, productId={}, sku={}",
                    request.getUserId(), request.getProductId(), request.getSku());
            return ResponseEntity.ok(ApiResponse.success("Item added to wishlist successfully", wishlist));
        } catch (RuntimeException ex) {
            log.error("[WISHLIST] Failed to add item | userId={}, productId={} | reason={}",
                    request.getUserId(), request.getProductId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to add item to wishlist: " + ex.getMessage()));
        }
    }

    // DELETE /api/v1/wishlist/remove?userId=1&productId=10&variantId=VAR-GOLD
    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(required = false) String variantId) {
        log.info("[WISHLIST] Remove item | userId={}, productId={}, variantId={}", userId, productId, variantId);
        try {
            wishlistService.removeItem(userId, productId, variantId);
            log.info("[WISHLIST] Item removed | userId={}, productId={}, variantId={}", userId, productId, variantId);
            return ResponseEntity.ok(ApiResponse.success("Item removed from wishlist successfully"));
        } catch (RuntimeException ex) {
            log.warn("[WISHLIST] Remove failed | userId={}, productId={} | reason={}", userId, productId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to remove item: " + ex.getMessage()));
        }
    }

    // GET /api/v1/wishlist/check?userId=1&productId=10&variantId=VAR-GOLD
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(required = false) String variantId) {
        log.info("[WISHLIST] Check wishlist | userId={}, productId={}, variantId={}", userId, productId, variantId);
        try {
            boolean exists = wishlistService.isInWishlist(userId, productId, variantId);
            log.info("[WISHLIST] Check result | userId={}, productId={}, inWishlist={}", userId, productId, exists);
            String msg = exists ? "Product is in wishlist" : "Product is not in wishlist";
            return ResponseEntity.ok(ApiResponse.success(msg, exists));
        } catch (RuntimeException ex) {
            log.warn("[WISHLIST] Check failed | userId={}, productId={} | reason={}", userId, productId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check wishlist status: " + ex.getMessage()));
        }
    }

    // DELETE /api/v1/wishlist/clear?userId=1&wishlistName=My Wishlist
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearWishlist(
            @RequestParam Long userId,
            @RequestParam(required = false) String wishlistName) {
        String resolvedName = wishlistName != null ? wishlistName : "My Wishlist";
        log.info("[WISHLIST] Clear wishlist | userId={}, wishlistName={}", userId, resolvedName);
        try {
            wishlistService.clearWishlist(userId, wishlistName);
            log.info("[WISHLIST] Wishlist cleared | userId={}, wishlistName={}", userId, resolvedName);
            return ResponseEntity.ok(ApiResponse.success("Wishlist '" + resolvedName + "' cleared successfully"));
        } catch (RuntimeException ex) {
            log.warn("[WISHLIST] Clear failed | userId={}, wishlistName={} | reason={}",
                    userId, resolvedName, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Wishlist '" + resolvedName + "' not found for userId: " + userId));
        }
    }


    // GET /api/v1/wishlist/count?userId=1
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<CountResponse>> getWishlistCount(@RequestParam Long userId) {
        log.info("[WISHLIST] Fetching wishlist count | userId={}", userId);
        try {
            int count = wishlistService.getWishlistCount(userId);
            log.info("[WISHLIST] Wishlist count fetched | userId={}, count={}", userId, count);
            return ResponseEntity.ok(ApiResponse.success("Wishlist count fetched successfully",
                    CountResponse.builder().userId(userId).count(count).build()));
        } catch (RuntimeException ex) {
            log.warn("[WISHLIST] Wishlist count failed | userId={} | reason={}", userId, ex.getMessage());
            // return 0 instead of error — safe for frontend badge
            return ResponseEntity.ok(ApiResponse.success("No wishlist found",
                    CountResponse.builder().userId(userId).count(0).build()));
        }
    }
}