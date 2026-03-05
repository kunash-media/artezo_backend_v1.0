package com.artezo.service;

import com.artezo.entity.InventoryEntity;

public interface InventoryService {

    InventoryEntity getBySku(String sku);

    void updateStock(String sku, Integer newAvailableStock);

    boolean hasEnoughStock(String sku, Integer requiredQuantity);
}