package com.artezo.service.serviceImpl;


import com.artezo.dto.request.InventoryDTO;
import com.artezo.dto.request.InventoryProductDTO;
import com.artezo.entity.InventoryEntity;
import com.artezo.entity.ProductEntity;
import com.artezo.entity.ProductVariantEntity;
import com.artezo.repository.InventoryRepository;
import com.artezo.repository.ProductRepository;
import com.artezo.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
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
    public void updateStock(String sku, Integer newAvailableStock, Integer rootStock) {
        InventoryEntity inv = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + sku));

        // ── Update InventoryEntity ────────────────────────────────
        inv.setAvailableStock(newAvailableStock);
        inv.setTotalStock(newAvailableStock);
        inventoryRepository.save(inv);
        log.info("Inventory updated → SKU: {}, availableStock: {}", sku, newAvailableStock);

        if (inv.getProduct() != null) {
            ProductEntity product = inv.getProduct();

            // ── Update variant stock ──────────────────────────────
            product.getVariants().stream()
                    .filter(v -> v.getSku().equals(sku))
                    .findFirst()
                    .ifPresent(variant -> {
                        variant.setStock(newAvailableStock);
                        log.info("Variant stock synced → SKU: {}, stock: {}", sku, newAvailableStock);
                    });

            // ── Update root product stock if rootStock param provided
            if (rootStock != null) {
                product.setCurrentStock(rootStock);
                log.info("Root product stock synced → productStrId: {}, stock: {}",
                        product.getProductStrId(), rootStock);
            }

            productRepository.save(product);
        }
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

        InventoryProductDTO productDTO = new InventoryProductDTO();
        InventoryDTO dto = new InventoryDTO();

        dto.setInventoryId(inventory.getInventoryId());
        dto.setSku(inventory.getSku());
        dto.setLowStockThreshold(inventory.getLowStockThreshold());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());

        // Convert product if exists (this happens within the transaction)
        if (inventory.getProduct() != null) {

            productDTO.setProductPrimeId(inventory.getProduct().getProductPrimeId());
            productDTO.setProductStrId(inventory.getProduct().getProductStrId());
            productDTO.setTitle(inventory.getProduct().getProductName());
            productDTO.setCurrentSku(inventory.getProduct().getCurrentSku());
            productDTO.setProductCategory(inventory.getProduct().getProductCategory());
            productDTO.setProductSubCategory(inventory.getProduct().getProductSubCategory());
            productDTO.setCurrentStock(inventory.getProduct().getCurrentStock());
            productDTO.setCurrentMrpPrice(inventory.getProduct().getCurrentMrpPrice());
            productDTO.setCurrentSellingPrice(inventory.getProduct().getCurrentSellingPrice());

            // find variant based on SKU
            String inventorySku = inventory.getSku();
            boolean isVariant = false;

            inventory.getProduct().getVariants()
                    .stream()
                    .filter(v -> v.getSku().equals(inventorySku))
                    .findFirst()
                    .ifPresent(variant -> {
                        productDTO.setVariantTitle(variant.getTitleName());
                        productDTO.setVariantId(variant.getVariantId());    // ← ADD
                        productDTO.setVariantStock(variant.getStock());     // ← ADD variant's own stock
                        productDTO.setVariantMrpPrice(variant.getMrp());
                        productDTO.setVariantSellingPrice(variant.getPrice());

                    });

            // ✅ Set flag — if sku matches any variant it's variant stock, else root
            productDTO.setIsVariantStock(
                    inventory.getProduct().getVariants()
                            .stream()
                            .anyMatch(v -> v.getSku().equals(inventorySku))
            );

            dto.setProduct(productDTO);

        }

        // ✅ Calculate AFTER productDTO is fully populated
        int totalStock = (productDTO.getVariantStock() != null ? productDTO.getVariantStock() : 0)
                + (productDTO.getCurrentStock() != null ? productDTO.getCurrentStock() : 0);
        dto.setTotalStock(totalStock);

        return dto;
    }
}