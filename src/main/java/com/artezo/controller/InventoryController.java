package com.artezo.controller;

import com.artezo.entity.InventoryEntity;
import com.artezo.repository.InventoryRepository;
import com.artezo.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryService inventoryService, InventoryRepository inventoryRepository) {
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
    }

    // GET single by SKU
    @GetMapping("/{sku}")
    public ResponseEntity<InventoryEntity> getInventory(@PathVariable String sku) {
        log.info("Fetching inventory for SKU: {}", sku);
        return ResponseEntity.ok(inventoryService.getBySku(sku));
    }

    // PATCH - update stock
    @PatchMapping("/{sku}/stock")
    public ResponseEntity<InventoryEntity> updateStock(
            @PathVariable String sku,
            @RequestBody Integer newStock) {

        log.info("Updating stock for SKU: {} to {}", sku, newStock);
        inventoryService.updateStock(sku, newStock);
        return ResponseEntity.ok(inventoryService.getBySku(sku));
    }

    // POST - create new inventory entry (manual / rare)
    @PostMapping
    public ResponseEntity<InventoryEntity> createInventory(@RequestBody InventoryEntity request) {
        log.info("Creating inventory entry for SKU: {}", request.getSku());
        InventoryEntity saved = inventoryRepository.save(request);
        return ResponseEntity.ok(saved);
    }

    // DELETE - remove inventory entry (careful - usually soft delete or archive)
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> deleteInventory(@PathVariable String sku) {
        log.warn("Deleting inventory for SKU: {}", sku);
        inventoryRepository.findBySku(sku).ifPresent(inventoryRepository::delete);
        return ResponseEntity.noContent().build();
    }
}