package com.artezo.dto.request;

import java.util.List;

public class RemoveCartItemsRequest {

    private Long userId;
    private List<CartItemIdentifier> items;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<CartItemIdentifier> getItems() { return items; }
    public void setItems(List<CartItemIdentifier> items) { this.items = items; }


    public static class CartItemIdentifier {
        private Long productId;
        private String variantId; // nullable

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getVariantId() { return variantId; }
        public void setVariantId(String variantId) { this.variantId = variantId; }
    }
}