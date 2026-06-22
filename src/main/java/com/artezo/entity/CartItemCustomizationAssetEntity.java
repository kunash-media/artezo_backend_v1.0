package com.artezo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Join table: one CartItem → multiple CustomizationAssets
 * slot_number=1 is always the primary asset (also stored as FK in CartItem)
 * slot_number=2,3,4 are additional uploaded images
 */
@Entity
@Table(name = "cart_item_customization_assets")
public class CartItemCustomizationAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItemEntity cartItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private CustomizationAssetEntity asset;

    // 1 = primary, 2 = second image, 3 = third ...
    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    // field name from customFields JSON e.g. "upload image - 2"
    @Column(name = "field_name", length = 255)
    private String fieldName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public CartItemCustomizationAssetEntity() {}

    // ── Getters & Setters ─────────────────────────────────────────────────
    public Long getId() { return id; }

    public CartItemEntity getCartItem() { return cartItem; }
    public void setCartItem(CartItemEntity cartItem) { this.cartItem = cartItem; }

    public CustomizationAssetEntity getAsset() { return asset; }
    public void setAsset(CustomizationAssetEntity asset) { this.asset = asset; }

    public Integer getSlotNumber() { return slotNumber; }
    public void setSlotNumber(Integer slotNumber) { this.slotNumber = slotNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}