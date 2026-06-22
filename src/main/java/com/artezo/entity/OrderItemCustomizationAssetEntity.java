package com.artezo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Join table: one OrderItem → multiple CustomizationAssets
 * Copied from CartItem assets when order is confirmed.
 * Admin panel fetches all slots for printing/processing.
 */
@Entity
@Table(name = "order_item_customization_assets")
public class OrderItemCustomizationAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItemEntity orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private CustomizationAssetEntity asset;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Column(name = "field_name", length = 255)
    private String fieldName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public OrderItemCustomizationAssetEntity() {}

    // ── Getters & Setters ─────────────────────────────────────────────────
    public Long getId() { return id; }

    public OrderItemEntity getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItemEntity orderItem) { this.orderItem = orderItem; }

    public CustomizationAssetEntity getAsset() { return asset; }
    public void setAsset(CustomizationAssetEntity asset) { this.asset = asset; }

    public Integer getSlotNumber() { return slotNumber; }
    public void setSlotNumber(Integer slotNumber) { this.slotNumber = slotNumber; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}