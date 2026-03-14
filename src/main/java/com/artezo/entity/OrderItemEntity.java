package com.artezo.entity;

import com.artezo.enum_status.ItemStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    // ── IDENTITY ─────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    // ── PRODUCT SNAPSHOT ──────────────────────────────────────────────────────
    // Stored as snapshot at time of order — completely decoupled from
    // ProductEntity so price/name changes don't affect order history.

    private String productStrId;        // PRD00001 — reference only (not FK)
    private String productName;         // snapshot
    private String brandName;           // snapshot

    private String sku;                 // variant sku or root sku
    private String variantId;           // VAR-GOLD, VAR-BLACK — null if no variant
    private String color;               // snapshot
    private String size;                // snapshot

    private String hsnCode;             // snapshot — needed for Shiprocket item payload

    // ── QUANTITY & PRICING SNAPSHOT ───────────────────────────────────────────

    private Integer quantity;

    private Double mrpPrice;            // original MRP snapshot
    private Double sellingPrice;        // price at which sold snapshot
    private Double discount;            // mrpPrice - sellingPrice (per unit)
    private Double itemTotal;           // sellingPrice × quantity

    // ── SHIPPING DIMENSION SNAPSHOT ───────────────────────────────────────────
    // Snapshot at time of order — used to recalculate shipping if needed.

    private Double weight;              // kg
    private Double length;              // cm
    private Double breadth;             // cm
    private Double height;              // cm

    // ── ITEM LEVEL STATUS ─────────────────────────────────────────────────────
    // Useful for partial returns / partial exchanges in multi-item orders.

    @Enumerated(EnumType.STRING)
    private ItemStatus itemStatus;      // ACTIVE, RETURN_REQUESTED, RETURNED,
    // EXCHANGE_REQUESTED, EXCHANGED, CANCELLED

    @CreationTimestamp
    private LocalDateTime createdAt;

    public OrderItemEntity() {
    }


    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public String getProductStrId() {
        return productStrId;
    }

    public void setProductStrId(String productStrId) {
        this.productStrId = productStrId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getMrpPrice() {
        return mrpPrice;
    }

    public void setMrpPrice(Double mrpPrice) {
        this.mrpPrice = mrpPrice;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(Double itemTotal) {
        this.itemTotal = itemTotal;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getBreadth() {
        return breadth;
    }

    public void setBreadth(Double breadth) {
        this.breadth = breadth;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public ItemStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(ItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

