package com.artezo.service.serviceImpl;

import com.artezo.entity.InventoryEntity;
import com.artezo.repository.InventoryRepository;
import com.artezo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryEntity getBySku(String sku) {
        return inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + sku));
    }

    @Override
    @Transactional
    public void updateStock(String sku, Integer newAvailableStock) {
        InventoryEntity inv = getBySku(sku);
        inv.setAvailableStock(newAvailableStock);
        inv.setTotalStock(newAvailableStock); // simplistic - adjust if you have reserved
        inventoryRepository.save(inv);
        log.info("Stock updated → SKU: {}, new available: {}", sku, newAvailableStock);
    }

    @Override
    public boolean hasEnoughStock(String sku, Integer requiredQuantity) {
        InventoryEntity inv = inventoryRepository.findBySku(sku).orElse(null);
        return inv != null && inv.getAvailableStock() >= requiredQuantity;
    }
}