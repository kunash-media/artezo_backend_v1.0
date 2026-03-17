package com.artezo.dto.request;

import java.time.LocalDateTime;


public class InventoryDTO {
    private Long inventoryId;
    private String sku;
    private Integer totalStock;
    private Integer lowStockThreshold;
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

//    public Integer getAvailableStock() {
//        return availableStock;
//    }
//
//    public void setAvailableStock(Integer availableStock) {
//        this.availableStock = availableStock;
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
