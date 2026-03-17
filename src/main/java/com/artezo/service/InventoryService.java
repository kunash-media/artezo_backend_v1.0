package com.artezo.service;

import com.artezo.dto.request.InventoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryDTO getBySku(String sku);

    public void updateStock(String sku, Integer newAvailableStock, Integer rootStock);

    boolean hasEnoughStock(String sku, Integer requiredQuantity);

    Page<InventoryDTO> getAllInventories(Pageable pageable);

}