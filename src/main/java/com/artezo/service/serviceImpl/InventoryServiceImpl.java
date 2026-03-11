package com.artezo.service.serviceImpl;


import com.artezo.dto.request.InventoryDTO;
import com.artezo.dto.request.InventoryProductDTO;
import com.artezo.entity.InventoryEntity;
import com.artezo.repository.InventoryRepository;
import com.artezo.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDTO getBySku(String sku) {
        InventoryEntity inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + sku));

        return convertToDTO(inventory);
    }

    @Override
    @Transactional
    public void updateStock(String sku, Integer newAvailableStock) {
        InventoryEntity inv = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + sku));

        inv.setAvailableStock(newAvailableStock);
        inv.setTotalStock(newAvailableStock); // simplistic - adjust if you have reserved
        inventoryRepository.save(inv);
        log.info("Stock updated → SKU: {}, new available: {}", sku, newAvailableStock);
    }



    @Override
    @Transactional(readOnly = true)
    public Page<InventoryDTO> getAllInventories(Pageable pageable) {
        log.info("Fetching all inventories with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        return inventoryRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEnoughStock(String sku, Integer requiredQuantity) {
        return inventoryRepository.findBySku(sku)
                .map(inv -> inv.getAvailableStock() >= requiredQuantity)
                .orElse(false);
    }

    private InventoryDTO convertToDTO(InventoryEntity inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setInventoryId(inventory.getInventoryId());
        dto.setSku(inventory.getSku());
        dto.setAvailableStock(inventory.getAvailableStock());
        dto.setTotalStock(inventory.getTotalStock());
        dto.setLowStockThreshold(inventory.getLowStockThreshold());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());

        // Convert product if exists (this happens within the transaction)
        if (inventory.getProduct() != null) {

            InventoryProductDTO productDTO = new InventoryProductDTO();
            productDTO.setProductPrimeId(inventory.getProduct().getProductPrimeId());
            productDTO.setProductStrId(inventory.getProduct().getProductStrId());
            productDTO.setTitle(inventory.getProduct().getProductName());
            productDTO.setCurrentSku(inventory.getProduct().getCurrentSku());
            productDTO.setProductCategory(inventory.getProduct().getProductCategory());
            productDTO.setProductSubCategory(inventory.getProduct().getProductSubCategory());

            // find variant based on SKU
            String inventorySku = inventory.getSku();

            inventory.getProduct().getVariants()
                    .stream()
                    .filter(v -> v.getSku().equals(inventorySku))
                    .findFirst()
                    .ifPresent(variant -> {
                        productDTO.setVariantTitle(variant.getTitleName());
                    });
            dto.setProduct(productDTO);

        }

        return dto;
    }
}