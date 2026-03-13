package com.artezo.service.serviceImpl;

import com.artezo.dto.request.AddToCartRequest;
import com.artezo.dto.response.CartResponse;
import com.artezo.entity.CartEntity;
import com.artezo.entity.CartItemEntity;
import com.artezo.entity.WishlistItemEntity;
import com.artezo.repository.CartItemRepository;
import com.artezo.repository.CartRepository;
import com.artezo.repository.WishlistItemRepository;
import com.artezo.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;


    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository, WishlistItemRepository wishlistItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistItemRepository = wishlistItemRepository;
    }

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        // get or create active cart
        CartEntity cart = getOrCreateCart(request.getUserId(), request.getSessionId());

        // check if same product+variant already in cart → update quantity
        Optional<CartItemEntity> existing = cartItemRepository
                .findByCart_IdAndProductIdAndVariantId(cart.getId(), request.getProductId(), request.getVariantId());

        if (existing.isPresent()) {
            CartItemEntity item = existing.get();
            item.setQuantity(item.getQuantity() + (request.getQuantity() != null ? request.getQuantity() : 1));
            cartItemRepository.save(item);
        } else {
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .variantId(request.getVariantId())
                    .sku(request.getSku())
                    .selectedColor(request.getSelectedColor())
                    .selectedSize(request.getSelectedSize())
                    .titleName(request.getTitleName())
                    .unitPrice(request.getUnitPrice())
                    .mrpPrice(request.getMrpPrice())
                    .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                    .customFieldsJson(request.getCustomFieldsJson())
                    .build();
            cartItemRepository.save(newItem);
        }

        return buildCartResponse(cart.getId(), cart.getUserId(), cart.getStatus().name());
    }

    @Override
    public CartResponse getCart(Long userId) {
        CartEntity cart = cartRepository
                .findByUserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active cart found for user: " + userId));
        return buildCartResponse(cart.getId(), cart.getUserId(), cart.getStatus().name());
    }

    @Override
    public CartResponse getCartBySession(String sessionId) {
        CartEntity cart = cartRepository
                .findBySessionIdAndStatus(sessionId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active cart found for session: " + sessionId));
        return buildCartResponse(cart.getId(), cart.getUserId(), cart.getStatus().name());
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, int quantity) {
        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + itemId));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        CartEntity cart = cartRepository.findByUserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        return buildCartResponse(cart.getId(), userId, cart.getStatus().name());
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId, String variantId) {
        CartEntity cart = cartRepository.findByUserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        cartItemRepository.deleteByCart_IdAndProductIdAndVariantId(cart.getId(), productId, variantId);
        return buildCartResponse(cart.getId(), userId, cart.getStatus().name());
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        CartEntity cart = cartRepository.findByUserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public CartResponse moveWishlistItemToCart(Long userId, Long wishlistItemId) {
        // fetch wishlist item
        WishlistItemEntity wishlistItem = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found: " + wishlistItemId));

        // direct mapping wishlist → cart (same fields, no conflict)
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

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CartEntity getOrCreateCart(Long userId, String sessionId) {
        if (userId != null) {
            return cartRepository.findByUserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                    .orElseGet(() -> cartRepository.save(
                            CartEntity.builder().userId(userId).build()));
        }
        return cartRepository.findBySessionIdAndStatus(sessionId, CartEntity.CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(
                        CartEntity.builder().sessionId(sessionId).build()));
    }

    private CartResponse buildCartResponse(Long cartId, Long userId, String status) {
        List<CartItemEntity> items = cartItemRepository.findByCart_Id(cartId);

        List<CartResponse.CartItemResponse> itemResponses = items.stream()
                .map(item -> CartResponse.CartItemResponse.builder()
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
                        .build())
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

    @Override
    public int getCartCount(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartEntity.CartStatus.ACTIVE)
                .map(cart -> {
                    Integer sum = cartItemRepository.sumQuantityByCartId(cart.getId());
                    return sum != null ? sum : 0;
                })
                .orElse(0);
    }

}