package com.artezo.service;

import com.artezo.dto.request.InventoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryDTO getBySku(String sku);

    void updateStock(String sku, Integer newAvailableStock);

    boolean hasEnoughStock(String sku, Integer requiredQuantity);

    Page<InventoryDTO> getAllInventories(Pageable pageable);

}