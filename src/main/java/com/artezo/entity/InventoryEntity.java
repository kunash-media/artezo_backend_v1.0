package com.artezo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    // Key connection to product/variant – must match ProductVariantEntity.sku or root currentSku
    @Column(nullable = false, unique = true)
    private String sku;                         // ART-WPLATE-GLD, ART-WPLATE-BLK, etc.

    // Optional: link to product (for queries)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_prime_id")
    private ProductEntity product;

    private Integer availableStock;             // what customer can buy right now

    private Integer reservedStock;              // locked for pending orders/cart (optional but useful)

    private Integer totalStock;                 // available + reserved + damaged/in-transit etc.

    private Integer lowStockThreshold;          // e.g. 10 → trigger alert when available <= this

    private Boolean backorderAllowed;           // allow orders when out of stock?

    private LocalDateTime lastRestockedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // new field added for inventory

    public InventoryEntity(){}

    public InventoryEntity(Long inventoryId, String sku, ProductEntity product, Integer availableStock, Integer reservedStock, Integer totalStock, Integer lowStockThreshold, Boolean backorderAllowed, LocalDateTime lastRestockedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.inventoryId = inventoryId;
        this.sku = sku;
        this.product = product;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
        this.totalStock = totalStock;
        this.lowStockThreshold = lowStockThreshold;
        this.backorderAllowed = backorderAllowed;
        this.lastRestockedAt = lastRestockedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(Integer reservedStock) {
        this.reservedStock = reservedStock;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Boolean getBackorderAllowed() {
        return backorderAllowed;
    }

    public void setBackorderAllowed(Boolean backorderAllowed) {
        this.backorderAllowed = backorderAllowed;
    }

    public LocalDateTime getLastRestockedAt() {
        return lastRestockedAt;
    }

    public void setLastRestockedAt(LocalDateTime lastRestockedAt) {
        this.lastRestockedAt = lastRestockedAt;
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