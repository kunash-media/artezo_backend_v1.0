package com.artezo.controller;

import com.artezo.dto.request.InventoryDTO;
import com.artezo.entity.InventoryEntity;
import com.artezo.repository.InventoryRepository;
import com.artezo.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // GET single by SKU - Now returns DTO
    @GetMapping("/get-by-sku/{sku}")
    public ResponseEntity<InventoryDTO> getInventory(@PathVariable String sku) {
        log.info("Fetching inventory for SKU: {}", sku);
        return ResponseEntity.ok(inventoryService.getBySku(sku));
    }

//    // PATCH - update stock
//    @PatchMapping("/patch-by-sku/{sku}/stock")
//    public ResponseEntity<InventoryDTO> updateStock(
//            @PathVariable String sku,
//            @RequestParam("availableStock") Integer newStock) {
//
//        log.info("Updating stock for SKU: {} to {}", sku, newStock);
//        inventoryService.updateStock(sku, newStock);
//        return ResponseEntity.ok(inventoryService.getBySku(sku));
//    }

    @PatchMapping("/patch-by-sku/{sku}/stock")
    public ResponseEntity<InventoryDTO> updateStock(
            @PathVariable String sku,
            @RequestParam(value = "availableStock",required = false) Integer newStock,
            @RequestParam(value = "rootStock", required = false) Integer rootStock) {

        log.info("Updating stock for SKU: {} → variantStock: {}, rootStock: {}", sku, newStock, rootStock);
        inventoryService.updateStock(sku, newStock, rootStock);
        return ResponseEntity.ok(inventoryService.getBySku(sku));
    }


    @GetMapping("/get-all-inventories")
    public ResponseEntity<Page<InventoryDTO>> getAllInventories(
            @PageableDefault(size = 10, sort = "sku", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("Fetching all inventories with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<InventoryDTO> inventories = inventoryService.getAllInventories(pageable);
        return ResponseEntity.ok(inventories);
    }


    // POST - create new inventory entry (manual / rare)
    @PostMapping
    public ResponseEntity<InventoryEntity> createInventory(@RequestBody InventoryEntity request) {
        log.info("Creating inventory entry for SKU: {}", request.getSku());
        InventoryEntity saved = inventoryRepository.save(request);
        return ResponseEntity.ok(saved);
    }

    // DELETE - remove inventory entry
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> deleteInventory(@PathVariable String sku) {
        log.warn("Deleting inventory for SKU: {}", sku);
        inventoryRepository.findBySku(sku).ifPresent(inventoryRepository::delete);
        return ResponseEntity.noContent().build();
    }
}