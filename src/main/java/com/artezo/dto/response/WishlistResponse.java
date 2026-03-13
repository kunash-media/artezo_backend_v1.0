package com.artezo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public class WishlistResponse {
    private Long wishlistId;
    private Long userId;
    private String name;
    private Boolean isPublic;
    private List<WishlistItemResponse> items;
    private int totalItems;


    public Long getWishlistId() {
        return wishlistId;
    }

    public void setWishlistId(Long wishlistId) {
        this.wishlistId = wishlistId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public List<WishlistItemResponse> getItems() {
        return items;
    }

    public void setItems(List<WishlistItemResponse> items) {
        this.items = items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    @Builder
    public static class WishlistItemResponse {
        private Long itemId;
        private Long productId;
        private String variantId;
        private String sku;
        private String selectedColor;
        private String selectedSize;
        private String titleName;
        private BigDecimal wishlistedPrice;
        private String customFieldsJson;
        private LocalDateTime addedAt;


        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getVariantId() {
            return variantId;
        }

        public void setVariantId(String variantId) {
            this.variantId = variantId;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getSelectedColor() {
            return selectedColor;
        }

        public void setSelectedColor(String selectedColor) {
            this.selectedColor = selectedColor;
        }

        public String getSelectedSize() {
            return selectedSize;
        }

        public void setSelectedSize(String selectedSize) {
            this.selectedSize = selectedSize;
        }

        public String getTitleName() {
            return titleName;
        }

        public void setTitleName(String titleName) {
            this.titleName = titleName;
        }

        public BigDecimal getWishlistedPrice() {
            return wishlistedPrice;
        }

        public void setWishlistedPrice(BigDecimal wishlistedPrice) {
            this.wishlistedPrice = wishlistedPrice;
        }

        public String getCustomFieldsJson() {
            return customFieldsJson;
        }

        public void setCustomFieldsJson(String customFieldsJson) {
            this.customFieldsJson = customFieldsJson;
        }

        public LocalDateTime getAddedAt() {
            return addedAt;
        }

        public void setAddedAt(LocalDateTime addedAt) {
            this.addedAt = addedAt;
        }
    }
}