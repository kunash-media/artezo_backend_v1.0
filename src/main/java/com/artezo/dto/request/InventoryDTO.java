package com.artezo.dto.request;

import java.time.LocalDateTime;


public class InventoryDTO {
    private Long inventoryId;
    private String sku;
    private Integer availableStock;
//    private Integer reservedStock;
    private Integer totalStock;
    private Integer lowStockThreshold;
//    private Boolean backorderAllowed;
//    private LocalDateTime lastRestockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Only include product info you need
    private InventoryProductDTO product;

    public InventoryDTO(){}

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

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

//    public Integer getReservedStock() {
//        return reservedStock;
//    }
//
//    public void setReservedStock(Integer reservedStock) {
//        this.reservedStock = reservedStock;
//    }

    public void setProduct(InventoryProductDTO product) {
        this.product = product;
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

//    public Boolean getBackorderAllowed() {
//        return backorderAllowed;
//    }
//
//    public void setBackorderAllowed(Boolean backorderAllowed) {
//        this.backorderAllowed = backorderAllowed;
//    }

//    public LocalDateTime getLastRestockedAt() {
//        return lastRestockedAt;
//    }
//
//    public void setLastRestockedAt(LocalDateTime lastRestockedAt) {
//        this.lastRestockedAt = lastRestockedAt;
//    }

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


    public InventoryProductDTO getProduct() {
        return product;
    }
}
