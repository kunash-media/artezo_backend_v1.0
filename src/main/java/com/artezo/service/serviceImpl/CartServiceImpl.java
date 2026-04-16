package com.artezo.service.serviceImpl;

import com.artezo.dto.request.AddToCartRequest;
import com.artezo.dto.response.CartResponse;
import com.artezo.entity.CartEntity;
import com.artezo.entity.CartItemEntity;
import com.artezo.entity.UserEntity;
import com.artezo.entity.WishlistItemEntity;
import com.artezo.repository.CartItemRepository;
import com.artezo.repository.CartRepository;
import com.artezo.repository.UserRepository;
import com.artezo.repository.WishlistItemRepository;
import com.artezo.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           WishlistItemRepository wishlistItemRepository,
                           UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        logger.info("[CART] Adding item to cart | userId={}, productId={}", request.getUserId(), request.getProductId());

        CartEntity cart = getOrCreateCart(request.getUserId(), request.getSessionId());

        Optional<CartItemEntity> existing = cartItemRepository
                .findByCart_IdAndProductIdAndVariantId(cart.getId(), request.getProductId(), request.getVariantId());

        if (existing.isPresent()) {
            CartItemEntity item = existing.get();
            item.setQuantity(item.getQuantity() + (request.getQuantity() != null ? request.getQuantity() : 1));
            cartItemRepository.save(item);
            logger.info("[CART] Quantity updated for existing item | itemId={}", item.getId());
        } else {
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .variantId(request.getVariantId())
                    .sku(request.getSku())
                    .selectedColor(request.getSelectedColor())
                    .selectedSize(request.getSelectedSize())
                    .titleName(request.getProductName() != null ? request.getProductName() : request.getTitleName())
                    .unitPrice(request.getUnitPrice())
                    .mrpPrice(request.getMrpPrice())
                    .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                    .customFieldsJson(request.getCustomFieldsJson())
                    .productImageUrl(calculateImageUrl(request.getProductId()))
                    .build();
            cartItemRepository.save(newItem);
            logger.info("[CART] New item added to cart | productId={}", request.getProductId());
        }

        Long userId = cart.getUser() != null ? cart.getUser().getUserId() : null;
        return buildCartResponse(cart.getId(), userId, cart.getStatus().name());
    }

    private String calculateImageUrl(Long productPrimeId) {
        if (productPrimeId == null) return null;
        return "/api/products/" + productPrimeId + "/main";
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        logger.info("[CART] Fetching cart | userId={}", userId);
        CartEntity cart = cartRepository
                .findByUser_UserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active cart found for user: " + userId));
        return buildCartResponse(cart.getId(), cart.getUser().getUserId(), cart.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartBySession(String sessionId) {
        logger.info("[CART] Fetching cart | sessionId={}", sessionId);
        CartEntity cart = cartRepository
                .findBySessionIdAndStatus(sessionId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active cart found for session: " + sessionId));
        Long userId = cart.getUser() != null ? cart.getUser().getUserId() : null;
        return buildCartResponse(cart.getId(), userId, cart.getStatus().name());
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, int quantity) {
        logger.info("[CART] Updating quantity | userId={}, itemId={}, quantity={}", userId, itemId, quantity);

        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + itemId));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            logger.info("[CART] Item removed (qty=0) | itemId={}", itemId);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        CartEntity cart = cartRepository.findByUser_UserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Cart not found for userId: " + userId));
        return buildCartResponse(cart.getId(), userId, cart.getStatus().name());
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId, String variantId) {
        logger.info("[CART] Removing item | userId={}, productId={}, variantId={}", userId, productId, variantId);

        CartEntity cart = cartRepository.findByUser_UserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Cart not found for userId: " + userId));

        cartItemRepository.deleteByCart_IdAndProductIdAndVariantId(cart.getId(), productId, variantId);
        return buildCartResponse(cart.getId(), userId, cart.getStatus().name());
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        logger.info("[CART] Clearing cart | userId={}", userId);

        CartEntity cart = cartRepository.findByUser_UserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Cart not found for userId: " + userId));

        cart.getItems().clear();
        cartRepository.save(cart);
        logger.info("[CART] Cart cleared | userId={}", userId);
    }

    @Override
    @Transactional
    public CartResponse moveWishlistItemToCart(Long userId, Long wishlistItemId) {
        logger.info("[CART] Moving wishlist item to cart | userId={}, wishlistItemId={}", userId, wishlistItemId);

        WishlistItemEntity wishlistItem = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found: " + wishlistItemId));

        AddToCartRequest request = new AddToCartRequest();
        request.setUserId(userId);
        request.setProductId(wishlistItem.getProductId());
        request.setVariantId(wishlistItem.getVariantId());
        request.setSku(wishlistItem.getSku());
        request.setSelectedColor(wishlistItem.getSelectedColor());
        request.setSelectedSize(wishlistItem.getSelectedSize());
        request.setTitleName(wishlistItem.getTitleName());
        request.setUnitPrice(wishlistItem.getWishlistedPrice());
        request.setCustomFieldsJson(wishlistItem.getCustomFieldsJson());
        request.setQuantity(1);

        return addToCart(request);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartCount(Long userId) {
        return cartRepository.findByUser_UserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .map(cart -> {
                    Integer sum = cartItemRepository.sumQuantityByCartId(cart.getId());
                    return sum != null ? sum : 0;
                })
                .orElse(0);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private CartEntity getOrCreateCart(Long userId, String sessionId) {
        if (userId != null) {
            return cartRepository.findByUser_UserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                    .orElseGet(() -> {
                        UserEntity user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                        logger.info("[CART] Creating new cart for userId={}", userId);
                        return cartRepository.save(
                                CartEntity.builder()
                                        .user(user)
                                        .status(CartEntity.CartStatus.ACTIVE)
                                        .build());
                    });
        }

        return cartRepository.findBySessionIdAndStatus(sessionId, CartEntity.CartStatus.ACTIVE)
                .orElseGet(() -> {
                    logger.info("[CART] Creating new guest cart for sessionId={}", sessionId);
                    return cartRepository.save(
                            CartEntity.builder()
                                    .sessionId(sessionId)
                                    .status(CartEntity.CartStatus.ACTIVE)
                                    .build());
                });
    }

    private CartResponse buildCartResponse(Long cartId, Long userId, String status) {
        List<CartItemEntity> items = cartItemRepository.findByCart_Id(cartId);

        List<CartResponse.CartItemResponse> itemResponses = items.stream()
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

                    return CartResponse.CartItemResponse.builder()
                            .itemId(item.getId())
                            .productId(item.getProductId())
                            .variantId(item.getVariantId())
                            .sku(item.getSku())
                            .selectedColor(item.getSelectedColor())
                            .selectedSize(item.getSelectedSize())
                            .titleName(item.getTitleName())
                            .unitPrice(item.getUnitPrice())
                            .mrpPrice(item.getMrpPrice())
                            .quantity(item.getQuantity())
                            .itemTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .customFieldsJson(item.getCustomFieldsJson())
                            .createdAt(item.getCreatedAt())
                            .productImageUrl(imageUrl)          // ← Always populated
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMrp = items.stream()
                .map(i -> i.getMrpPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cartId)
                .userId(userId)
                .status(status)
                .items(itemResponses)
                .totalItems(items.stream().mapToInt(CartItemEntity::getQuantity).sum())
                .totalAmount(totalAmount)
                .totalMrp(totalMrp)
                .totalDiscount(totalMrp.subtract(totalAmount))
                .build();
    }
}