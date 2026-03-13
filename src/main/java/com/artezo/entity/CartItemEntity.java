package com.artezo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id", "variant_id"}))
@Builder
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // nullable for products without variants
    @Column(name = "variant_id")
    private String variantId;

    // snapshot fields from variant at time of add
    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "selected_color")
    private String selectedColor;

    @Column(name = "selected_size")
    private String selectedSize;

    @Column(name = "title_name")
    private String titleName;

    // price snapshots — protects against price changes
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "mrp_price", precision = 10, scale = 2)
    private BigDecimal mrpPrice;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // stores customFields JSON if product isCustomizable
    @Column(name = "custom_fields_json", columnDefinition = "TEXT")
    private String customFieldsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CartItemEntity(){}

    public CartItemEntity(Long id, CartEntity cart, Long productId, String variantId, String sku, String selectedColor, String selectedSize, String titleName, BigDecimal unitPrice, BigDecimal mrpPrice, Integer quantity, String customFieldsJson, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.cart = cart;
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.selectedColor = selectedColor;
        this.selectedSize = selectedSize;
        this.titleName = titleName;
        this.unitPrice = unitPrice;
        this.mrpPrice = mrpPrice;
        this.quantity = quantity;
        this.customFieldsJson = customFieldsJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CartEntity getCart() {
        return cart;
    }

    public void setCart(CartEntity cart) {
        this.cart = cart;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getMrpPrice() {
        return mrpPrice;
    }

    public void setMrpPrice(BigDecimal mrpPrice) {
        this.mrpPrice = mrpPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCustomFieldsJson() {
        return customFieldsJson;
    }

    public void setCustomFieldsJson(String customFieldsJson) {
        this.customFieldsJson = customFieldsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}