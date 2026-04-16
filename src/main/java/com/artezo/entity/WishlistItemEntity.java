package com.artezo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wishlist_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wishlist_id", "product_id", "variant_id"})
)
@Builder
public class WishlistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_id", nullable = false)
    private WishlistEntity wishlist;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private String variantId;

    // snapshot fields
    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "selected_color")
    private String selectedColor;

    @Column(name = "selected_size")
    private String selectedSize;

    @Column(name = "title_name")
    private String titleName;

    // price at wishlist time — used for "Price Dropped!" notification
    @Column(name = "wishlisted_price", precision = 10, scale = 2)
    private BigDecimal wishlistedPrice;

    @Column(name = "custom_fields_json", columnDefinition = "TEXT")
    private String customFieldsJson;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    public WishlistItemEntity(){}

    public WishlistItemEntity(Long id, WishlistEntity wishlist, Long productId, String variantId, String sku, String selectedColor, String selectedSize, String titleName, BigDecimal wishlistedPrice, String customFieldsJson, LocalDateTime addedAt, String productImageUrl) {
        this.id = id;
        this.wishlist = wishlist;
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.selectedColor = selectedColor;
        this.selectedSize = selectedSize;
        this.titleName = titleName;
        this.wishlistedPrice = wishlistedPrice;
        this.customFieldsJson = customFieldsJson;
        this.addedAt = addedAt;
        this.productImageUrl = productImageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WishlistEntity getWishlist() {
        return wishlist;
    }

    public void setWishlist(WishlistEntity wishlist) {
        this.wishlist = wishlist;
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

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }
}