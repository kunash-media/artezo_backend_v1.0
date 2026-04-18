package com.artezo.service.serviceImpl;

import com.artezo.dto.request.*;
import com.artezo.dto.response.*;
import com.artezo.entity.*;
import com.artezo.exceptions.ProductAlreadyDeletedException;
import com.artezo.exceptions.ProductCreateResult;
import com.artezo.exceptions.ProductNotFoundException;
import com.artezo.repository.InstallationStepRepository;
import com.artezo.repository.InventoryRepository;
import com.artezo.repository.ProductRepository;
import com.artezo.service.ProductService;
import com.artezo.service.RecentViewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InstallationStepRepository installationStepRepository;
    private final InventoryRepository inventoryRepository;
    private final RecentViewService recentViewService;

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductServiceImpl(ProductRepository productRepository, InstallationStepRepository installationStepRepository, InventoryRepository inventoryRepository, RecentViewService recentViewService) {
        this.productRepository = productRepository;
        this.installationStepRepository = installationStepRepository;
        this.inventoryRepository = inventoryRepository;
        this.recentViewService = recentViewService;
    }

    // ────────────────────────────────────────────────
    //             URL generators
    // ────────────────────────────────────────────────
    private String mainImageUrl(Long productPrimeId) {
        return "/api/products/" + productPrimeId + "/main";
    }

    private String mockupImageUrl(Long productPrimeId, int index) {
        return "/api/products/" + productPrimeId + "/mockup/" + index;
    }

    private String variantMainImageUrl(Long productPrimeId, String variantId) {
        return "/api/products/" + productPrimeId + "/variant/" + variantId + "/main";
    }

    // ────────────────────────────────────────────────
    //            CREATE PRODUCT + INVENTORY SYNC
    // ────────────────────────────────────────────────
    @Override
    @Transactional
    public ProductCreateResult createProduct(CreateProductRequestDto request) {
        log.info("Creating product → name: {}, hasVariants: {}",
                request.getProductName(), request.getHasVariants());

        // ─────── DUPLICATE CHECKS → return proper error instead of null ───────
        if (request.getProductName() != null && productRepository.existsByProductName(request.getProductName())) {
            return ProductCreateResult.error(
                    "Product with name '" + request.getProductName() + "' already exists",
                    HttpStatus.CONFLICT);
        }

        if (request.getCurrentSku() != null && inventoryRepository.findBySku(request.getCurrentSku()).isPresent()) {
            return ProductCreateResult.error(
                    "SKU '" + request.getCurrentSku() + "' already exists",
                    HttpStatus.CONFLICT);
        }

        if (request.getVariants() != null) {
            for (VariantRequestDto v : request.getVariants()) {
                if (v.getSku() != null && inventoryRepository.findBySku(v.getSku()).isPresent()) {
                    return ProductCreateResult.error(
                            "Variant SKU '" + v.getSku() + "' already exists",
                            HttpStatus.CONFLICT);
                }
            }
        }

        // ─────── Rest of your original create logic (unchanged) ───────
        ProductEntity entity = new ProductEntity();

        mapBaseFieldsToEntity(entity, request);

        // Handle images
        entity.setMainImageData(request.getMainImage());

        if (request.getMockupImages() != null && !request.getMockupImages().isEmpty()) {
            entity.getMockupImageDataList().addAll(request.getMockupImages());
        }

        // ─── NEW: Main product video (optional) ───
        entity.setProductVideoData(request.getProductVideo());

        // Handle variants (temporarily without variantId)

        // Step 1: add variants WITHOUT mockups
        if (Boolean.TRUE.equals(request.getHasVariants()) && request.getVariants() != null) {
            for (VariantRequestDto vReq : request.getVariants()) {
                ProductVariantEntity variant = new ProductVariantEntity();
                mapVariantToEntity(variant, vReq);
                variant.setProduct(entity);
                // NO mockups here yet — IDs don't exist
                entity.getVariants().add(variant);
            }
        }



        // Step 2: first save — gets variant IDs assigned
        long count = productRepository.count() + 1;
        entity.setProductStrId(String.format("PRD%05d", count));
        ProductEntity saved = productRepository.saveAndFlush(entity);
        log.info("Product created → productPrimeId: {}, productStrId: {}", saved.getProductPrimeId(), saved.getProductStrId());

        // Step 3: generate variantIds + assign mockups NOW that IDs exist
        if (saved.getVariants() != null && !saved.getVariants().isEmpty()) {
            for (int i = 0; i < saved.getVariants().size(); i++) {
                ProductVariantEntity variant = saved.getVariants().get(i);

                // Generate variantId
                String color = variant.getColor() != null ? variant.getColor().trim() : "";
                String colorPart = color.isEmpty() ? "DEFAULT" :
                        color.toUpperCase().replaceAll("[^A-Z0-9]", "");
                variant.setVariantId(String.format("VAR-%s-%d", colorPart, variant.getId()));

                // ── Assign mockups using OneToMany entity — ID exists now ──
                if (request.getVariants() != null && i < request.getVariants().size()) {
                    VariantRequestDto vReq = request.getVariants().get(i);
                    if (vReq.getMockupImages() != null && !vReq.getMockupImages().isEmpty()) {
                        for (byte[] bytes : vReq.getMockupImages()) {
                            VariantMockupImageEntity img = new VariantMockupImageEntity(variant, bytes);
                            variant.getMockupImages().add(img);
                        }
                    }
                }
            }
            saved = productRepository.save(saved);
        }

        // Save installation steps
        saveInstallationSteps(saved, request);

        // INVENTORY SYNC
        syncInventoryFromProduct(saved);

        ProductResponseDto responseDto = mapToResponseDto(saved);
        return ProductCreateResult.success(responseDto);
    }

    // Get product video
    @Cacheable(value = "productVideos", key = "#productPrimeId")
    @Override
    public byte[] getProductVideoData(Long productPrimeId) {
        return productRepository.findById(productPrimeId)
                .map(ProductEntity::getProductVideoData)
                .orElse(null);
    }


    @Override
    public ProductResponseDto getAdminViewProductById(Long productPrimeId) {
        ProductEntity entity = productRepository.findById(productPrimeId)
                .orElseThrow(() -> new RuntimeException("Product not found with productPrimeId: " + productPrimeId));
        return mapToResponseDto(entity);
    }

    @Override
    @Transactional
    public Page<ProductResponseDto> getAllActiveProducts(int page, int size, String sortBy, String sortDir) {
        // Default page size = 8, clamp to reasonable values
        int effectiveSize = (size <= 0 || size > 100) ? 10 : size;

        // Default sort: productPrimeId DESC if nothing provided
        Sort sort = Sort.by(
                Sort.Direction.fromString(sortDir != null && !sortDir.isBlank() ? sortDir.toUpperCase() : "DESC"),
                sortBy != null && !sortBy.isBlank() ? sortBy : "productPrimeId"
        );

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                effectiveSize,
                sort
        );

        // Fetch the page of active products
        Page<ProductEntity> productPage = productRepository.findAllActiveProductsOrdered(pageable);

        // Manual conversion: entity → DTO
        return productPage.map(this::mapToListResponseDto);
    }


    /**
     * Lightweight mapping for product list / catalog views.
     * Skips: faq, specifications, additionalInfo, heroBanners, installationSteps,
     *        globalTags, addonKeys, description, aboutItem, full variant details
     */
    private ProductResponseDto mapToListResponseDto(ProductEntity e) {
        ProductResponseDto dto = new ProductResponseDto();

        dto.setProductPrimeId(e.getProductPrimeId());
        dto.setProductStrId(e.getProductStrId());
        dto.setProductName(e.getProductName());
        dto.setBrandName(e.getBrandName());
        dto.setProductCategory(e.getProductCategory());
        dto.setProductSubCategory(e.getProductSubCategory());

        dto.setIsDeleted(e.getIsDeleted());
        dto.setHasVariants(e.getHasVariants());
        dto.setIsCustomizable(e.getIsCustomizable());
        dto.setIsExchange(e.getIsExchange());
        dto.setReturnAvailable(e.getReturnAvailable());
        dto.setUnderTrendCategory(e.getUnderTrendCategory());

        dto.setCurrentSku(e.getCurrentSku());
        dto.setSelectedColor(e.getSelectedColor());
        dto.setCurrentSellingPrice(e.getCurrentSellingPrice());
        dto.setCurrentMrpPrice(e.getCurrentMrpPrice());
        dto.setCurrentStock(e.getCurrentStock());
        dto.setWeight(e.getWeight());
        dto.setLength(e.getLength());
        dto.setBreadth(e.getBreadth());
        dto.setHeight(e.getHeight());

        if (e.getMainImageData() != null && e.getMainImageData().length > 0) {
            dto.setMainImage(mainImageUrl(e.getProductPrimeId()));
        }

        dto.setYoutubeUrl(e.getYoutubeUrl());

        // ✅ Added: lightweight variant stock for list view
        if (e.getHasVariants() && e.getVariants() != null && !e.getVariants().isEmpty()) {
            List<VariantResponseDto> variantsDto = e.getVariants().stream()
                    .map(v -> {
                        VariantResponseDto vd = new VariantResponseDto();
                        vd.setVariantId(v.getVariantId());
                        vd.setTitleName(v.getTitleName());
                        vd.setColor(v.getColor());
                        vd.setSku(v.getSku());
                        vd.setStock(v.getStock());
                        vd.setPrice(v.getPrice());
                        vd.setMrp(v.getMrp());
                        return vd;
                    })
                    .collect(Collectors.toList());

            dto.setAvailableVariants(variantsDto);
            dto.setVariantCount(variantsDto.size());
        }

        return dto;
    }

    // ────────────────────────────────────────────────
    //                  UPDATE FULL + INVENTORY SYNC
    @Override
    @Transactional
    public ProductResponseDto patchProduct(Long productPrimeId, CreateProductRequestDto request) {
        log.info("Partial patch product productPrimeId: {}", productPrimeId);

        ProductEntity entity = productRepository.findById(productPrimeId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productPrimeId));

        // ── Basic fields ──
        if (request.getProductName() != null) entity.setProductName(request.getProductName());
        if (request.getBrandName() != null) entity.setBrandName(request.getBrandName());
        if (request.getProductCategory() != null) entity.setProductCategory(request.getProductCategory());
        if (request.getProductSubCategory() != null) entity.setProductSubCategory(request.getProductSubCategory());
        if (request.getYoutubeUrl() != null) entity.setYoutubeUrl(request.getYoutubeUrl());

        // ── Boolean flags — null-safe ──
        if (request.getHasVariants() != null) entity.setHasVariants(request.getHasVariants());
        if (request.getIsCustomizable() != null) entity.setIsCustomizable(request.getIsCustomizable());
        if (request.getIsExchange() != null) entity.setIsExchange(request.getIsExchange());
        if (request.getReturnAvailable() != null) entity.setReturnAvailable(request.getReturnAvailable());
        if (request.getUnderTrendCategory() != null) entity.setUnderTrendCategory(request.getUnderTrendCategory());

        // ── Pricing & stock ──
        if (request.getCurrentSku() != null) entity.setCurrentSku(request.getCurrentSku());
        if (request.getSelectedColor() != null) entity.setSelectedColor(request.getSelectedColor());
        if (request.getCurrentSellingPrice() != null) entity.setCurrentSellingPrice(request.getCurrentSellingPrice());
        if (request.getCurrentMrpPrice() != null) entity.setCurrentMrpPrice(request.getCurrentMrpPrice());
        if (request.getCurrentStock() != null) entity.setCurrentStock(request.getCurrentStock());

        // ── Text lists ──
        if (request.getDescription() != null) entity.setDescription(String.join("\n", request.getDescription()));
        if (request.getAboutItem() != null) entity.setAboutItem(String.join("\n", request.getAboutItem()));

        // ── JSON fields ──
        if (request.getSpecifications() != null) {
            try {
                entity.setSpecifications(objectMapper.writeValueAsString(request.getSpecifications()));
            } catch (Exception ex) {
                log.error("Failed to serialize specifications", ex);
            }
        }

        if (request.getCustomFields() != null) {
            try {
                entity.setCustomFields(objectMapper.writeValueAsString(request.getCustomFields()));
            } catch (Exception ex) {
                log.error("Failed to serialize customFields", ex);
            }
        }

        if (request.getAdditionalInfo() != null) {
            try {
                entity.setAdditionalInfo(objectMapper.writeValueAsString(request.getAdditionalInfo()));
            } catch (Exception ex) {
                log.error("Failed to serialize additionalInfo", ex);
            }
        }
        if (request.getFaq() != null) {
            try {
                entity.setFaq(objectMapper.writeValueAsString(request.getFaq()));
            } catch (Exception ex) {
                log.error("Failed to serialize faq", ex);
            }
        }
        if (request.getGlobalTags() != null) {
            try {
                entity.setGlobalTags(objectMapper.writeValueAsString(request.getGlobalTags()));
            } catch (Exception ex) {
                log.error("Failed to serialize globalTags", ex);
            }
        }
        if (request.getAddonKeys() != null) {
            try {
                entity.setAddonKeys(objectMapper.writeValueAsString(request.getAddonKeys()));
            } catch (Exception ex) {
                log.error("Failed to serialize addonKeys", ex);
            }
        }
        if (request.getCategoryPath() != null && !request.getCategoryPath().isEmpty()) {
            entity.setCategoryPath(request.getCategoryPath());
        }

        // ── Images ──
        if (request.getMainImage() != null) entity.setMainImageData(request.getMainImage());
        if (request.getMockupImages() != null) {
            entity.getMockupImageDataList().clear();
            entity.getMockupImageDataList().addAll(request.getMockupImages());
        }
        if (request.getProductVideo() != null) entity.setProductVideoData(request.getProductVideo());

        // ── Hero banners — merge by bannerId ──
        if (request.getHeroBanners() != null) {
            try {
                List<HeroBannerRequestDto> existing = new ArrayList<>();
                if (entity.getHeroBanners() != null && !"[]".equals(entity.getHeroBanners())) {
                    existing = objectMapper.readValue(entity.getHeroBanners(),
                            new TypeReference<List<HeroBannerRequestDto>>() {
                            });
                }
                for (HeroBannerRequestDto incoming : request.getHeroBanners()) {
                    Optional<HeroBannerRequestDto> match = existing.stream()
                            .filter(b -> b.getBannerId() == incoming.getBannerId())
                            .findFirst();
                    if (match.isPresent()) {
                        if (incoming.getImgDescription() != null)
                            match.get().setImgDescription(incoming.getImgDescription());
                        if (incoming.getBannerImg() != null) match.get().setBannerImg(incoming.getBannerImg());
                    } else {
                        existing.add(incoming);
                    }
                }
                entity.setHeroBanners(objectMapper.writeValueAsString(existing));
            } catch (Exception ex) {
                log.error("Patch hero banners failed", ex);
            }
        }

        // ── Installation steps — patch per step number ──
        if (request.getInstallationSteps() != null) {
            for (InstallationStepRequestDto stepReq : request.getInstallationSteps()) {
                Optional<InstallationStepEntity> existing = Optional.ofNullable(installationStepRepository
                        .findByProduct_ProductPrimeIdAndStep(entity.getProductPrimeId(), stepReq.getStep()));

                InstallationStepEntity stepEntity = existing.orElse(new InstallationStepEntity());
                if (stepEntity.getId() == null) stepEntity.setProduct(entity);

                stepEntity.setStep(stepReq.getStep());
                if (stepReq.getTitle() != null) stepEntity.setTitle(stepReq.getTitle());
                if (stepReq.getShortDescription() != null)
                    stepEntity.setShortDescription(stepReq.getShortDescription());
                if (stepReq.getShortNote() != null) stepEntity.setShortNote(stepReq.getShortNote());
                if (stepReq.getStepImage() != null) stepEntity.setStepImageData(stepReq.getStepImage());
                if (stepReq.getVideoFile() != null) stepEntity.setVideoData(stepReq.getVideoFile());

                installationStepRepository.save(stepEntity);
            }
        }

        // ── Variants — full replace if provided ──

        if (request.getVariants() != null) {

            // ✅ FIX: snapshot BOTH mainImage AND mockups by index (not SKU)
            // Index-based is safer — SKU may change or be null
            List<byte[]> existingMainImages = new ArrayList<>();
            List<List<byte[]>> existingMockupsList = new ArrayList<>();

            for (ProductVariantEntity existing : entity.getVariants()) {
                existingMainImages.add(existing.getMainImageData());
                existingMockupsList.add(
                        existing.getMockupImages().stream()
                                .map(VariantMockupImageEntity::getImageData)
                                .collect(Collectors.toList())
                );
            }

            // Clear and flush
            entity.getVariants().clear();
            productRepository.saveAndFlush(entity);

            // Rebuild variants
            for (VariantRequestDto vReq : request.getVariants()) {
                ProductVariantEntity variant = new ProductVariantEntity();
                mapVariantToEntity(variant, vReq); // mainImage only set if non-null (Fix 1)
                variant.setProduct(entity);
                entity.getVariants().add(variant);
            }

            // Save to get IDs
            entity = productRepository.saveAndFlush(entity);



            // Assign mockups + restore main images where not overridden
            for (int i = 0; i < entity.getVariants().size(); i++) {

                ProductVariantEntity savedVariant = entity.getVariants().get(i);
                VariantRequestDto vReq = request.getVariants().get(i);

                // ✅ FIX: restore existing mainImage if no new one was sent
                if (savedVariant.getMainImageData() == null && i < existingMainImages.size()) {
                    savedVariant.setMainImageData(existingMainImages.get(i));
                }

                // Determine mockup bytes to use
                List<byte[]> mockupBytes = null;

                // Temporary — add inside the variants loop, before mockup assignment
                log.info("Variant[{}] SKU={} incomingMockups={} existingMockups={}",
                        i,
                        vReq.getSku(),
                        vReq.getMockupImages() != null ? vReq.getMockupImages().size() : 0,
                        i < existingMockupsList.size() ? existingMockupsList.get(i).size() : 0
                );

                if (vReq.getMockupImages() != null && !vReq.getMockupImages().isEmpty()) {
                    // New mockups sent — use them
                    mockupBytes = vReq.getMockupImages();
                } else if (!Boolean.TRUE.equals(vReq.getClearMockupImages())) {
                    // ✅ FIX: fall back by index first, then by SKU as secondary
                    if (i < existingMockupsList.size() && !existingMockupsList.get(i).isEmpty()) {
                        mockupBytes = existingMockupsList.get(i);
                    }
                }

                if (mockupBytes != null) {
                    for (byte[] bytes : mockupBytes) {
                        VariantMockupImageEntity img = new VariantMockupImageEntity(savedVariant, bytes);
                        savedVariant.getMockupImages().add(img);
                    }
                }
            }

            // Final save
            entity = productRepository.saveAndFlush(entity);
        }

        // Regenerate variantIds
        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            for (ProductVariantEntity variant : entity.getVariants()) {
                String color = variant.getColor() != null ? variant.getColor().trim() : "";
                String colorPart = color.isEmpty() ? "DEFAULT" :
                        color.toUpperCase().replaceAll("[^A-Z0-9]", "");
                variant.setVariantId(String.format("VAR-%s-%d", colorPart, variant.getId()));
            }
            entity = productRepository.save(entity);
        }

        if (request.getCurrentStock() != null || request.getVariants() != null) {
            syncInventoryFromProduct(entity);
        }

        return mapToResponseDto(entity);
    }


    // ────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long productPrimeId, CreateProductRequestDto request) {
        log.info("Full update product productPrimeId: {}", productPrimeId);

        ProductEntity entity = productRepository.findById(productPrimeId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productPrimeId));

        mapBaseFieldsToEntity(entity, request);

        // Replace images
        entity.setMainImageData(request.getMainImage());
        entity.getMockupImageDataList().clear();
        if (request.getMockupImages() != null) {
            entity.getMockupImageDataList().addAll(request.getMockupImages());
        }

        // Replace variants → regenerate IDs after save
        entity.getVariants().clear();
        if (Boolean.TRUE.equals(request.getHasVariants()) && request.getVariants() != null) {
            for (VariantRequestDto vReq : request.getVariants()) {
                ProductVariantEntity variant = new ProductVariantEntity();
                mapVariantToEntity(variant, vReq);
                variant.setProduct(entity);
                entity.getVariants().add(variant);
            }
        }

        ProductEntity saved = productRepository.save(entity);

        // Regenerate variantIds
        if (saved.getVariants() != null && !saved.getVariants().isEmpty()) {
            for (ProductVariantEntity variant : saved.getVariants()) {
                String color = variant.getColor() != null ? variant.getColor().trim() : "";
                String colorPart = color.isEmpty() ? "DEFAULT" :
                        color.toUpperCase().replaceAll("[^A-Z0-9]", "");

                String generatedId = String.format("VAR-%s-%d", colorPart, variant.getId());
                variant.setVariantId(generatedId);
            }
            saved = productRepository.save(saved);
        }

        // Update installation steps (delete old + save new)
        installationStepRepository.deleteByProduct_ProductPrimeId(saved.getProductPrimeId());
        saveInstallationSteps(saved, request);

        // ── INVENTORY SYNC ──
        syncInventoryFromProduct(saved);

        return mapToResponseDto(saved);
    }
    // ────────────────────────────────────────────────
    //                  PATCH + INVENTORY SYNC
    // ────────────────────────────────────────────────

    // ────────────────────────────────────────────────
    //      PRIVATE: Save installation steps
    // ────────────────────────────────────────────────
    private void saveInstallationSteps(ProductEntity product, CreateProductRequestDto request) {
        if (request.getInstallationSteps() != null && !request.getInstallationSteps().isEmpty()) {
            for (InstallationStepRequestDto stepReq : request.getInstallationSteps()) {
                InstallationStepEntity stepEntity = new InstallationStepEntity();
                stepEntity.setProduct(product);
                stepEntity.setStep(stepReq.getStep());
                stepEntity.setTitle(stepReq.getTitle());
                stepEntity.setShortDescription(stepReq.getShortDescription());
                stepEntity.setShortNote(stepReq.getShortNote());
                stepEntity.setStepImageData(stepReq.getStepImage());
                stepEntity.setVideoData(stepReq.getVideoFile());
                installationStepRepository.save(stepEntity);
            }
            log.info("Saved {} installation steps for product {}",
                    request.getInstallationSteps().size(), product.getProductPrimeId());
        }
    }

    // ────────────────────────────────────────────────
    //      PRIVATE: Sync inventory from product/variants
    // ────────────────────────────────────────────────
    private void syncInventoryFromProduct(ProductEntity product) {
        if (product.getCurrentSku() != null && product.getCurrentStock() != null) {
            InventoryEntity inv = inventoryRepository.findBySku(product.getCurrentSku())
                    .orElse(new InventoryEntity());
            inv.setSku(product.getCurrentSku());
            inv.setProduct(product);
            inv.setAvailableStock(product.getCurrentStock());
            inv.setTotalStock(product.getCurrentStock());
            inv.setLowStockThreshold(10);
            inventoryRepository.save(inv);
            log.info("Inventory synced for root SKU: {}, stock: {}", product.getCurrentSku(), product.getCurrentStock());
        }

        for (ProductVariantEntity v : product.getVariants()) {
            if (v.getSku() != null && v.getStock() != null) {
                InventoryEntity inv = inventoryRepository.findBySku(v.getSku())
                        .orElse(new InventoryEntity());
                inv.setSku(v.getSku());
                inv.setProduct(product);
                inv.setAvailableStock(v.getStock());
                inv.setTotalStock(v.getStock());
                inv.setLowStockThreshold(10);
                inventoryRepository.save(inv);
                log.info("Inventory synced for variant SKU: {}, stock: {}", v.getSku(), v.getStock());
            }
        }
    }

    // ────────────────────────────────────────────────
    //      get product by productPrimeId
    // ────────────────────────────────────────────────
//    @Override
//    public ProductResponseDto getProductById(Long productPrimeId) {
//        ProductEntity entity = productRepository.findById(productPrimeId)
//                .orElseThrow(() -> new RuntimeException("Product not found with productPrimeId: " + productPrimeId));
//        return mapToResponseDto(entity);
//    }

    @Override
    @Transactional
    public ProductResponseDto getProductById(Long productPrimeId, Long userId) {
        log.info("Fetching product for productPrimeId: {} | userId: {}", productPrimeId, userId);

        ProductEntity entity = productRepository.findById(productPrimeId)
                .orElseThrow(() -> {
                    log.warn("Product not found for productPrimeId: {}", productPrimeId);
                    return new RuntimeException("Product not found with productPrimeId: " + productPrimeId);
                });

        ProductResponseDto response = mapToResponseDto(entity);

        // Async Redis write — never blocks main response
        CompletableFuture.runAsync(() -> recentViewService.recordView(userId, response))
                .exceptionally(ex -> {
                    log.error("Async recent view recording failed for productPrimeId: {} | reason: {}",
                            productPrimeId, ex.getMessage());
                    return null;
                });

        return response;
    }


    @Override
    public ProductResponseDto getProductByStrId(String productStrId) {
        ProductEntity entity = productRepository.findByProductStrId(productStrId)
                .orElseThrow(() -> new RuntimeException("Product not found with strId: " + productStrId));
        return mapToResponseDto(entity);
    }

    @Override
    public void deleteProduct(Long productPrimeId) throws ProductAlreadyDeletedException, ProductNotFoundException {
        ProductEntity entity = productRepository.findById(productPrimeId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productPrimeId));

        if (entity.getIsDeleted()) {
            throw new ProductAlreadyDeletedException("Product with ID " + productPrimeId + " is already deleted");
        }

        entity.setIsDeleted(true);
        productRepository.save(entity);
    }

    @Override
    public byte[] getProductMainImageData(Long productPrimeId) {
        return productRepository.findById(productPrimeId)
                .map(ProductEntity::getMainImageData)
                .orElse(null);
    }

    @Override
    public List<byte[]> getProductMockupImagesData(Long productPrimeId) {
        return productRepository.findById(productPrimeId)
                .map(ProductEntity::getMockupImageDataList)
                .orElse(List.of());
    }

    @Override
    public byte[] getVariantMainImageData(Long productPrimeId, String variantId) {
        return productRepository.findById(productPrimeId)
                .flatMap(p -> p.getVariants().stream()
                        .filter(v -> variantId.equals(v.getVariantId()))
                        .findFirst()
                        .map(ProductVariantEntity::getMainImageData))
                .orElse(null);
    }


    // ────────────────────────────────────────────────
    //              Get Product Video
    // ────────────────────────────────────────────────
    @Cacheable(value = "installationVideos", key = "#productPrimeId + '-' + #stepIndex")
    @Override
    public byte[] getInstallationVideoData(Long productPrimeId, int stepIndex) {
        InstallationStepEntity step = installationStepRepository
                .findByProduct_ProductPrimeIdAndStep(productPrimeId, stepIndex);
        return (step != null) ? step.getVideoData() : null;
    }


    // ────────────────────────────────────────────────
    //      MAPPING HELPERS
    // ────────────────────────────────────────────────
    private void mapBaseFieldsToEntity(ProductEntity e, CreateProductRequestDto r) {
        e.setProductName(r.getProductName());
        e.setBrandName(r.getBrandName());
        e.setProductCategory(r.getProductCategory());
        e.setProductSubCategory(r.getProductSubCategory());
        e.setHasVariants(r.getHasVariants());
        e.setIsCustomizable(r.getIsCustomizable());
        e.setIsExchange(r.getIsExchange());
        e.setReturnAvailable(r.getReturnAvailable());
        e.setUnderTrendCategory(r.getUnderTrendCategory());

        e.setCurrentSku(r.getCurrentSku());
        e.setSelectedColor(r.getSelectedColor());
        e.setCurrentSellingPrice(r.getCurrentSellingPrice());
        e.setCurrentMrpPrice(r.getCurrentMrpPrice());
        e.setCurrentStock(r.getCurrentStock());
        e.setHsnCode(r.getHsnCode());
        e.setWeight(r.getWeight());
        e.setLength(r.getLength());
        e.setBreadth(r.getBreadth());
        e.setHeight(r.getHeight());

        if (r.getDescription() != null) {
            e.setDescription(String.join("\n", r.getDescription()));
        }
        if (r.getAboutItem() != null) {
            e.setAboutItem(String.join("\n", r.getAboutItem()));
        }

        ObjectMapper mapper = new ObjectMapper();

        if (r.getSpecifications() != null) {
            try {
                e.setSpecifications(mapper.writeValueAsString(r.getSpecifications()));
            } catch (Exception ex) {
                log.error("Failed to serialize specifications", ex);
                e.setSpecifications("{}");
            }
        }

        if (r.getCustomFields() != null) {
            try {
                e.setCustomFields(objectMapper.writeValueAsString(r.getCustomFields()));
            } catch (Exception ex) {
                log.error("Failed to serialize customFields", ex);
                e.setCustomFields("[]");
            }
        }

        if (r.getAdditionalInfo() != null) {
            try {
                e.setAdditionalInfo(mapper.writeValueAsString(r.getAdditionalInfo()));
            } catch (Exception ex) {
                log.error("Failed to serialize additionalInfo", ex);
                e.setAdditionalInfo("{}");
            }
        }

        if (r.getGlobalTags() != null) {
            try {
                e.setGlobalTags(mapper.writeValueAsString(r.getGlobalTags()));
            } catch (Exception ex) {
                log.error("Failed to serialize globalTags", ex);
                e.setGlobalTags("[]");
            }
        }

        if (r.getAddonKeys() != null) {
            try {
                e.setAddonKeys(mapper.writeValueAsString(r.getAddonKeys()));
            } catch (Exception ex) {
                log.error("Failed to serialize addonKeys", ex);
                e.setAddonKeys("[]");
            }
        }

        if (r.getFaq() != null) {
            try {
                e.setFaq(mapper.writeValueAsString(r.getFaq()));
            } catch (Exception ex) {
                log.error("Failed to serialize faqAns", ex);
                e.setFaq("{}");
            }
        }

        if (r.getHeroBanners() != null && !r.getHeroBanners().isEmpty()) {
            try {
                e.setHeroBanners(mapper.writeValueAsString(r.getHeroBanners()));
            } catch (Exception ex) {
                log.error("Failed to serialize heroBanners", ex);
                e.setHeroBanners("[]");
            }
        } else {
            e.setHeroBanners("[]");
        }
    }

    private void mapVariantToEntity(ProductVariantEntity v, VariantRequestDto r) {
        v.setTitleName(r.getTitleName());
        v.setColor(r.getColor());
        v.setSku(r.getSku());
        v.setPrice(r.getPrice());
        v.setMrp(r.getMrp());
        v.setStock(r.getStock());

        if (r.getMainImage() != null) {
            v.setMainImageData(r.getMainImage());
        }
        v.setMfgDate(r.getMfgDate());
        v.setExpDate(r.getExpDate());
        v.setSize(r.getSize());
        v.setWeight(r.getWeight());
        v.setBreadth(r.getBreadth());
        v.setHeight(r.getHeight());
        v.setLength(r.getLength());
        // variantId is NOT set here anymore — generated later
    }

    private ProductResponseDto mapToResponseDto(ProductEntity e) {
        ProductResponseDto dto = new ProductResponseDto();

        dto.setProductPrimeId(e.getProductPrimeId());
        dto.setProductStrId(e.getProductStrId());
        dto.setProductName(e.getProductName());
        dto.setBrandName(e.getBrandName());
        dto.setProductCategory(e.getProductCategory());
        dto.setProductSubCategory(e.getProductSubCategory());

        dto.setIsDeleted(e.getIsDeleted());
        dto.setHasVariants(e.getHasVariants());
        dto.setIsCustomizable(e.getIsCustomizable());
        dto.setIsExchange(e.getIsExchange());

        dto.setCurrentSku(e.getCurrentSku());
        dto.setSelectedColor(e.getSelectedColor());
        dto.setCurrentSellingPrice(e.getCurrentSellingPrice());
        dto.setCurrentMrpPrice(e.getCurrentMrpPrice());
        dto.setCurrentStock(e.getCurrentStock());
        dto.setYoutubeUrl(e.getYoutubeUrl());
        dto.setReturnAvailable(e.getReturnAvailable());
        dto.setUnderTrendCategory(e.getUnderTrendCategory());
        dto.setHsnCode(e.getHsnCode());
        dto.setBreadth(e.getBreadth());
        dto.setLength(e.getLength());
        dto.setHeight(e.getHeight());
        dto.setWeight(e.getWeight());

        if (e.getMainImageData() != null && e.getMainImageData().length > 0) {
            dto.setMainImage(mainImageUrl(e.getProductPrimeId()));
        }

        if (e.getProductVideoData() != null && e.getProductVideoData().length > 0) {
            dto.setProductVideoUrl("/api/products/" + e.getProductPrimeId() + "/product-video");
        }

        if (e.getMockupImageDataList() != null && !e.getMockupImageDataList().isEmpty()) {
            List<String> mockupUrls = new ArrayList<>();
            for (int i = 0; i < e.getMockupImageDataList().size(); i++) {
                byte[] img = e.getMockupImageDataList().get(i);
                if (img != null && img.length > 0) {
                    mockupUrls.add(mockupImageUrl(e.getProductPrimeId(), i));
                }
            }
            dto.setMockupImages(mockupUrls);
        }

        // REPLACE your existing variant stream with this:
        if (e.getHasVariants() && e.getVariants() != null && !e.getVariants().isEmpty()) {
            List<VariantResponseDto> variantsDto = e.getVariants().stream()
                    .map(v -> {
                        VariantResponseDto vd = new VariantResponseDto();
                        vd.setVariantId(v.getVariantId());
                        vd.setTitleName(v.getTitleName());
                        vd.setColor(v.getColor());
                        vd.setSku(v.getSku());
                        vd.setPrice(v.getPrice());
                        vd.setMrp(v.getMrp());
                        vd.setStock(v.getStock());
                        vd.setSize(v.getSize());                    // ← FIX: was missing
                        vd.setMfgDate(v.getMfgDate());              // ← optional but consistent
                        vd.setExpDate(v.getExpDate());              // ← optional but consistent
                        vd.setWeight(v.getWeight());
                        vd.setLength(v.getLength());
                        vd.setBreadth(v.getBreadth());
                        vd.setHeight(v.getHeight());

                        // Main image URL
                        if (v.getMainImageData() != null && v.getMainImageData().length > 0) {
                            vd.setMainImage(variantMainImageUrl(e.getProductPrimeId(), v.getVariantId()));
                        }

                        // ── FIX: variant mockup image URLs ──
                        if (v.getMockupImages() != null && !v.getMockupImages().isEmpty()) {
                            List<String> mockupUrls = new ArrayList<>();
                            for (int i = 0; i < v.getMockupImages().size(); i++) {
                                mockupUrls.add("/api/products/" + e.getProductPrimeId()
                                        + "/variant/" + v.getVariantId()
                                        + "/mockup/" + i);
                            }
                            vd.setMockupImages(mockupUrls);
                        }

                        return vd;
                    })
                    .collect(Collectors.toList());

            dto.setAvailableVariants(variantsDto);
        }

        // Hero banners
        if (e.getHeroBanners() != null && !"[]".equals(e.getHeroBanners())) {
            try {
                List<HeroBannerRequestDto> banners = objectMapper.readValue(
                        e.getHeroBanners(),
                        new TypeReference<List<HeroBannerRequestDto>>() {}
                );
                List<HeroBannerResponseDto> responseBanners = banners.stream().map(b -> {
                    HeroBannerResponseDto rb = new HeroBannerResponseDto();
                    rb.setBannerId(b.getBannerId());
                    rb.setImgDescription(b.getImgDescription());
                    if (b.getBannerImg() != null && b.getBannerImg().length > 0) {
                        rb.setBannerImg("/api/products/" + e.getProductPrimeId() + "/hero-banner/" + b.getBannerId());
                    }
                    return rb;
                }).collect(Collectors.toList());
                dto.setHeroBanners(responseBanners);
            } catch (Exception ex) {
                log.error("Failed to deserialize heroBanners JSON", ex);
                dto.setHeroBanners(List.of());
            }
        } else {
            dto.setHeroBanners(List.of());
        }

        // FAQ (faqAns)
        if (e.getFaq() != null && !"{}".equals(e.getFaq()) && !"null".equals(e.getFaq())) {
            try {
                Map<String, String> faqMap = objectMapper.readValue(
                        e.getFaq(),
                        new TypeReference<Map<String, String>>() {}
                );
                dto.setFaq(faqMap);
            } catch (Exception ex) {
                log.error("Failed to deserialize faq JSON", ex);
                dto.setFaq(new HashMap<>());
            }
        } else {
            dto.setFaq(new HashMap<>());
        }

        if (e.getSpecifications() != null && !"{}".equals(e.getSpecifications()) && !"null".equals(e.getSpecifications())) {
            try {
                Map<String, String> specificationsMap = objectMapper.readValue(
                        e.getSpecifications(),
                        new TypeReference<Map<String, String>>() {}
                );
                dto.setSpecifications(specificationsMap);
            } catch (Exception ex) {
                log.error("Failed to deserialize specifications JSON", ex);
                dto.setSpecifications(new HashMap<>());
            }
        } else {
            dto.setSpecifications(new HashMap<>());
        }

        dto.setCustomFields(e.getCustomFields());

        // Global tags
        if (e.getGlobalTags() != null && !"[]".equals(e.getGlobalTags())) {
            try {
                List<String> tags = objectMapper.readValue(
                        e.getGlobalTags(),
                        new TypeReference<List<String>>() {}
                );
                dto.setGlobalTags(tags);
            } catch (Exception ex) {
                log.error("Failed to deserialize globalTags JSON", ex);
                dto.setGlobalTags(List.of());
            }
        } else {
            dto.setGlobalTags(List.of());
        }

        // Addon keys
        if (e.getAddonKeys() != null && !"[]".equals(e.getAddonKeys())) {
            try {
                List<String> addons = objectMapper.readValue(
                        e.getAddonKeys(),
                        new TypeReference<List<String>>() {}
                );
                dto.setAddonKeys(addons);
            } catch (Exception ex) {
                log.error("Failed to deserialize addonKeys JSON", ex);
                dto.setAddonKeys(List.of());
            }
        } else {
            dto.setAddonKeys(List.of());
        }

        // Description & aboutItem (split back to list)
        if (e.getDescription() != null) {
            dto.setDescription(List.of(e.getDescription().split("\n")));
        }
        if (e.getAboutItem() != null) {
            dto.setAboutItem(List.of(e.getAboutItem().split("\n")));
        }

        // Installation steps
        List<InstallationStepEntity> steps = installationStepRepository
                .findByProduct_ProductPrimeId(e.getProductPrimeId());

        if (steps != null && !steps.isEmpty()) {
            List<InstallationStepResponseDto> stepDtos = steps.stream().map(s -> {
                InstallationStepResponseDto sd = new InstallationStepResponseDto();
                sd.setStep(s.getStep());
                sd.setTitle(s.getTitle());
                sd.setShortDescription(s.getShortDescription());
                sd.setShortNote(s.getShortNote());

                if (s.getStepImageData() != null && s.getStepImageData().length > 0) {
                    sd.setStepImage("/api/products/" + e.getProductPrimeId() + "/step/" + s.getStep() + "/image");
                }
                if (s.getVideoData() != null && s.getVideoData().length > 0) {
                    sd.setVideoUrl("/api/products/" + e.getProductPrimeId() + "/installation-video/" + s.getStep());
                }
                return sd;
            }).collect(Collectors.toList());

            dto.setInstallationSteps(stepDtos);
        } else {
            dto.setInstallationSteps(List.of());
        }

        return dto;
    }

    public byte[] getHeroBannerImage(Long productPrimeId, String bannerId) {
        ProductEntity product = productRepository.findById(productPrimeId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productPrimeId));

        String heroBannersJson = product.getHeroBanners();
        if (heroBannersJson == null || "[]".equals(heroBannersJson)) {
            return null;
        }

        try {
            List<HeroBannerRequestDto> banners = objectMapper.readValue(
                    heroBannersJson,
                    new TypeReference<List<HeroBannerRequestDto>>() {}
            );

            return banners.stream()
                    .filter(b -> bannerId.equals(String.valueOf(b.getBannerId())))
                    .map(HeroBannerRequestDto::getBannerImg)
                    .filter(img -> img != null && img.length > 0)
                    .findFirst()
                    .orElse(null);

        } catch (Exception ex) {
            log.error("Failed to read hero banner image for product {} banner {}", productPrimeId, bannerId, ex);
            return null;
        }
    }

    public byte[] getInstallationStepImage(Long productPrimeId, Integer step) {
        return installationStepRepository
                .findByProduct_ProductPrimeIdAndStep(productPrimeId, step)
                .map(InstallationStepEntity::getStepImageData)
                .filter(img -> img != null && img.length > 0)
                .orElse(null);
    }


    //==================================================
    //          Bulk Uploading
    //==================================================


    private static final String[] REQUIRED_COLUMNS = {
            "product name", "category", "sku", "current stock",
            "hsn code", "weight", "length", "breadth", "height"
    };


    @Transactional
    public BulkUploadResponse bulkCreateProducts(MultipartFile excelFile,
                                                 List<MultipartFile> images) {

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║         BULK PRODUCT UPLOAD STARTED");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        BulkUploadResponse response = new BulkUploadResponse();
        List<String> skippedReasons = new ArrayList<>();
        int uploadedCount = 0;
        int skippedCount  = 0;

        Map<String, MultipartFile> fileMap = buildFileMap(images);

        try (InputStream is = excelFile.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet     = workbook.getSheetAt(0);
            Row   headerRow = sheet.getRow(1);

            if (headerRow == null) {
                return failResponse(response, skippedReasons, "Excel file has no header row");
            }

            Map<String, Integer> col = buildColumnIndex(headerRow);
            log.info("Detected {} columns: {}", col.size(), col.keySet());

            List<String> missing = Arrays.stream(REQUIRED_COLUMNS)
                    .filter(req -> !col.containsKey(req))
                    .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                return failResponse(response, skippedReasons,
                        "Missing required column(s): " + String.join(", ", missing));
            }

            Iterator<Row> iterator = sheet.rowIterator();
            iterator.next(); // skip section label row (row 1)
            iterator.next(); // skip header row (row 2)

            int rowNumber = 1;

            while (iterator.hasNext()) {
                Row row = iterator.next();
                rowNumber++;

                if (isRowBlank(row, col)) {
                    log.debug("Skipping blank row {}", rowNumber);
                    continue;
                }

                try {

                    // ── MANDATORY ─────────────────────────────────────────────

                    String productName = str(row, col, "product name");
                    if (productName.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "Product name is empty");
                        continue;
                    }

                    String category = str(row, col, "category");
                    if (category.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "Category is empty — product: " + productName);
                        continue;
                    }

                    String sku = str(row, col, "sku");
                    if (sku.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "SKU is empty — product: " + productName);
                        continue;
                    }

                    // ── HSN CODE (mandatory) ───────────────────────────────────
                    String hsnCode = str(row, col, "hsn code");
                    if (hsnCode.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "HSN code is empty — product: " + productName);
                        continue;
                    }

                    // ── ROOT LEVEL DIMENSIONS (mandatory) ─────────────────────
                    // Parsed as Double — used for no-variant products directly,
                    // and as fallback for variant products if variant dimension is 0.

                    String weightStr = str(row, col, "weight");
                    if (weightStr.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "weight is empty — product: " + productName);
                        continue;
                    }
                    Double rootWeight = parseDouble(weightStr);

                    String lengthStr = str(row, col, "length");
                    if (lengthStr.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "length is empty — product: " + productName);
                        continue;
                    }
                    Double rootLength = parseDouble(lengthStr);

                    String breadthStr = str(row, col, "breadth");
                    if (breadthStr.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "breadth is empty — product: " + productName);
                        continue;
                    }
                    Double rootBreadth = parseDouble(breadthStr);

                    String heightStr = str(row, col, "height");
                    if (heightStr.isEmpty()) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber, "height is empty — product: " + productName);
                        continue;
                    }
                    Double rootHeight = parseDouble(heightStr);

                    int currentStock = intVal(row, col, "current stock", -1);
                    if (currentStock < 0) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber,
                                "Invalid or missing 'current stock' for: " + productName
                                        + " (must be a non-negative number)");
                        continue;
                    }

                    log.info("Processing row {} → '{}' | SKU: {}", rowNumber, productName, sku);

                    // ── DUPLICATE PRE-CHECKS ──────────────────────────────────
                    // (createProduct() also checks, but pre-checking here gives
                    //  a cleaner "Row N: ..." message in skippedReasons)

                    if (productRepository.existsByProductName(productName)) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber,
                                "Product name already exists: '" + productName + "'");
                        continue;
                    }
                    if (productRepository.existsByCurrentSku(sku)) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber,
                                "SKU already exists: '" + sku + "'");
                        continue;
                    }

                    if (productRepository.existsByHsnCode(hsnCode)) {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber,
                                "HSN code already exists: '" + hsnCode + "'");
                        continue;
                    }

                    // ── BASIC OPTIONAL ────────────────────────────────────────

                    String brand       = str(row, col, "brand");
                    String subCategory = str(row, col, "sub category");
                    String color       = str(row, col, "color");
                    String youtubeUrl  = str(row, col, "youtube url");

                    double sellingPrice = dbl(row, col, "selling price", 0.0);
                    double mrpPrice     = dbl(row, col, "mrp price", 0.0);
                    if (mrpPrice > 0 && mrpPrice < sellingPrice) {
                        log.warn("Row {} → MRP ({}) < selling price ({}) for '{}' — auto-corrected",
                                rowNumber, mrpPrice, sellingPrice, productName);
                        mrpPrice = sellingPrice;
                    }

                    boolean hasVariants     = bool(row, col, "has variants",    false);
                    boolean isCustomizable  = bool(row, col, "is customizable", false);
                    boolean isExchange      = bool(row, col, "is exchange",      false);
                    boolean underTrend      = bool(row, col, "under trend",      false);
                    boolean returnAvailable = bool(row, col, "return available", true);

                    // ── LIST<STRING> FIELDS  (semicolon → split → List<String>) ──
                    // These match the DTO fields: List<String> description, aboutItem, etc.

                    List<String> descriptionList = semicolonList(row, col, "description");
                    List<String> aboutItemList   = semicolonList(row, col, "about item");
                    List<String> globalTagsList  = semicolonList(row, col, "global tags");
                    List<String> addonKeysList   = semicolonList(row, col, "addon keys");
                    List<String> categoryPath    = semicolonList(row, col, "category path");

                    // ── MAP<STRING,STRING> FIELDS  (raw JSON object in cell) ───
                    // Cell value example: {"material":"Acrylic","weight":"250g"}
                    // Parsed into Map<String,String> — matching DTO type exactly.
                    // Invalid JSON → warn + null (row is NOT skipped).

                    Map<String, String> specifications = parseJsonMap(str(row, col, "specifications"), "specifications", productName);
                    Map<String, String> additionalInfo = parseJsonMap(str(row, col, "additional info"), "additional info", productName);
                    Map<String, String> faq            = parseJsonMap(str(row, col, "faq"),             "faq",            productName);

                    // String customFields = validatedJsonOrNull(str(row, col, "custom fields"), "custom fields", productName);

                    //FIX custom fields parsing
                    List<Object> customFields = parseJsonList(str(row, col, "custom fields"), "custom fields", productName);

                    // ── IMAGES ────────────────────────────────────────────────

                    byte[]       mainImageBytes = resolveBytes(fileMap, str(row, col, "main image"),    "main image", productName, rowNumber);
                    List<byte[]> mockupBytes    = resolveBytesListBySemicolon(fileMap,
                            semicolonList(row, col, "mockup images"), "mockup", productName, rowNumber);

                    // ── VARIANTS ──────────────────────────────────────────────

                    List<VariantRequestDto> variantDtos = new ArrayList<>();
                    if (hasVariants) {
                        List<String> varSkus     = semicolonList(row, col, "variant skus");
                        List<String> varColors   = semicolonList(row, col, "variant colors");
                        List<String> varTitles   = semicolonList(row, col, "variant titles");
                        List<String> varPrices   = semicolonList(row, col, "variant prices");
                        List<String> varMrps     = semicolonList(row, col, "variant mrps");
                        List<String> varStocks   = semicolonList(row, col, "variant stocks");
                        List<String> varSizes    = semicolonList(row, col, "variant sizes");
                        List<String> varMfgDates = semicolonList(row, col, "variant mfg dates");
                        List<String> varExpDates = semicolonList(row, col, "variant exp dates");
                        List<String> varWeight   = semicolonList(row, col, "variant weight");
                        List<String> varLength   = semicolonList(row, col, "variant length");
                        List<String> varBreadth  = semicolonList(row, col, "variant breadth");
                        List<String> varHeight   = semicolonList(row, col, "variant height");

                        List<String> varImgNames = semicolonList(row, col, "variant images");

                        if (varSkus.isEmpty()) {
                            log.warn("Row {} → hasVariants=true but 'variant skus' is empty for '{}' — no variants built",
                                    rowNumber, productName);
                        } else {
                            for (int i = 0; i < varSkus.size(); i++) {
                                String vSku = varSkus.get(i).trim();
                                if (vSku.isEmpty()) continue;

                                double vPrice  = parseDouble(safeGet(varPrices,  i, "0"));
                                double vMrp    = parseDouble(safeGet(varMrps,    i, "0"));
                                int    vStock  = parseInt(safeGet(varStocks,     i, "0"));

                                // ── VARIANT DIMENSIONS ────────────────────────────
                                // Parse each variant's own value.
                                // If 0 or missing → fall back to root-level dimension
                                // so Shiprocket always gets a valid non-zero value.
                                double vWeight  = parseDouble(safeGet(varWeight,  i, "0"));
                                double vLength  = parseDouble(safeGet(varLength,  i, "0"));
                                double vBreadth = parseDouble(safeGet(varBreadth, i, "0"));
                                double vHeight  = parseDouble(safeGet(varHeight,  i, "0"));

                                if (vMrp > 0 && vMrp < vPrice) {
                                    log.warn("Row {} → Variant '{}' MRP < price — auto-corrected", rowNumber, vSku);
                                    vMrp = vPrice;
                                }

                                VariantRequestDto v = new VariantRequestDto();
                                v.setSku(vSku);
                                v.setColor(safeGet(varColors,   i, color));   // fallback to root color
                                v.setTitleName(safeGet(varTitles, i, ""));
                                v.setSize(safeGet(varSizes,      i, "Standard"));
                                v.setMfgDate(parseLocalDate(safeGet(varMfgDates, i, null)));
                                v.setExpDate(parseLocalDate(safeGet(varExpDates,  i, null)));
                                v.setPrice(vPrice > 0 ? vPrice : null);
                                v.setMrp(vMrp > 0 ? vMrp : null);
                                v.setStock(vStock);

                                // ✅ FIXED: set parsed Double with root-level fallback
                                // (previously was incorrectly setting List<String> here)
                                v.setWeight(vWeight   > 0 ? vWeight   : rootWeight);
                                v.setLength(vLength   > 0 ? vLength   : rootLength);
                                v.setBreadth(vBreadth > 0 ? vBreadth  : rootBreadth);
                                v.setHeight(vHeight   > 0 ? vHeight   : rootHeight);

                                String vImgName = safeGet(varImgNames, i, "");
                                if (!vImgName.isEmpty()) {
                                    v.setMainImage(resolveBytes(fileMap, vImgName,
                                            "variant[" + i + "] image", productName, rowNumber));
                                }

                                variantDtos.add(v);
                                log.debug("Row {} → Variant: sku={}, color={}, stock={}, weight={}, dims={}x{}x{}",
                                        rowNumber, vSku, v.getColor(), vStock,
                                        v.getWeight(), v.getLength(), v.getBreadth(), v.getHeight());
                            }
                        }
                    }

                    // ── HERO BANNERS ──────────────────────────────────────────

                    List<HeroBannerRequestDto> bannerDtos  = new ArrayList<>();
                    List<String> bannerDescs    = semicolonList(row, col, "banner descriptions");
                    List<String> bannerImgNames = semicolonList(row, col, "banner images");

                    for (int i = 0; i < bannerDescs.size(); i++) {
                        HeroBannerRequestDto banner = new HeroBannerRequestDto();
                        banner.setBannerId(i + 1);
                        banner.setImgDescription(bannerDescs.get(i));
                        String bImg = safeGet(bannerImgNames, i, "");
                        if (!bImg.isEmpty()) {
                            banner.setBannerImg(resolveBytes(fileMap, bImg,
                                    "banner[" + i + "] image", productName, rowNumber));
                        }
                        bannerDtos.add(banner);
                    }

                    // ── INSTALLATION STEPS ────────────────────────────────────

                    List<InstallationStepRequestDto> stepDtos = new ArrayList<>();
                    List<String> stepTitles   = semicolonList(row, col, "step titles");
                    List<String> stepDescs    = semicolonList(row, col, "step descriptions");
                    List<String> stepNotes    = semicolonList(row, col, "step notes");
                    List<String> stepImgNames = semicolonList(row, col, "step images");
                    List<String> stepVidNames = semicolonList(row, col, "step videos");

                    for (int i = 0; i < stepTitles.size(); i++) {
                        InstallationStepRequestDto step = new InstallationStepRequestDto();
                        step.setStep(i + 1);
                        step.setTitle(stepTitles.get(i));
                        step.setShortDescription(safeGet(stepDescs, i, ""));
                        step.setShortNote(safeGet(stepNotes,        i, ""));
                        String sImg = safeGet(stepImgNames, i, "");
                        if (!sImg.isEmpty()) {
                            step.setStepImage(resolveBytes(fileMap, sImg,
                                    "step[" + i + "] image", productName, rowNumber));
                        }
                        String sVid = safeGet(stepVidNames, i, "");
                        if (!sVid.isEmpty()) {
                            step.setVideoFile(resolveBytes(fileMap, sVid,
                                    "step[" + i + "] video", productName, rowNumber));
                        }
                        stepDtos.add(step);
                    }

                    // ── BUILD DTO ─────────────────────────────────────────────

                    CreateProductRequestDto dto = new CreateProductRequestDto();

                    // Mandatory
                    dto.setProductName(productName);
                    dto.setProductCategory(category);
                    dto.setCurrentSku(sku);
                    dto.setCurrentStock(currentStock);

                    // Basic optional
                    dto.setProductSubCategory(nullIfEmpty(subCategory));
                    dto.setBrandName(nullIfEmpty(brand));
                    dto.setCurrentSellingPrice(sellingPrice > 0 ? sellingPrice : null);
                    dto.setCurrentMrpPrice(mrpPrice > 0 ? mrpPrice : null);
                    dto.setSelectedColor(nullIfEmpty(color));
                    dto.setYoutubeUrl(nullIfEmpty(youtubeUrl));

                    // Flags
                    dto.setHasVariants(hasVariants);
                    dto.setIsCustomizable(isCustomizable);
                    dto.setIsExchange(isExchange);
                    dto.setUnderTrendCategory(underTrend);
                    dto.setReturnAvailable(returnAvailable);

                    // ── SHIPPING FIELDS (mandatory for Shiprocket) ────────────
                    // Root-level dimensions: used directly when hasVariants=false,
                    // also stored as product-level fallback when hasVariants=true.
                    dto.setHsnCode(hsnCode);
                    dto.setWeight(rootWeight);
                    dto.setLength(rootLength);
                    dto.setBreadth(rootBreadth);
                    dto.setHeight(rootHeight);

                    // List<String> fields — set directly, no JSON conversion needed here.
                    // mapBaseFieldsToEntity() in ProductServiceImpl handles serialization.
                    dto.setDescription(descriptionList.isEmpty()  ? null : descriptionList);
                    dto.setAboutItem(aboutItemList.isEmpty()       ? null : aboutItemList);
                    dto.setGlobalTags(globalTagsList.isEmpty()     ? null : globalTagsList);
                    dto.setAddonKeys(addonKeysList.isEmpty()       ? null : addonKeysList);
                    dto.setCategoryPath(categoryPath.isEmpty()     ? null : categoryPath);

                    // Map<String, String> fields — set directly, no JSON conversion needed here.
                    // mapBaseFieldsToEntity() handles objectMapper.writeValueAsString() internally.
                    dto.setSpecifications(specifications);
                    dto.setAdditionalInfo(additionalInfo);
                    dto.setFaq(faq);
                    dto.setCustomFields(customFields);

                    // Images
                    dto.setMainImage(mainImageBytes);
                    dto.setMockupImages(mockupBytes.isEmpty() ? null : mockupBytes);
                    dto.setProductVideo(null); // video not supported in bulk

                    // Variants / banners / steps
                    dto.setVariants(variantDtos.isEmpty()       ? null : variantDtos);
                    dto.setHeroBanners(bannerDtos.isEmpty()     ? null : bannerDtos);
                    dto.setInstallationSteps(stepDtos.isEmpty() ? null : stepDtos);

                    // ── DELEGATE ──────────────────────────────────────────────
                    // productStrId is auto-generated inside createProduct() → PRD%05d
                    ProductCreateResult result = createProduct(dto);

                    if (result.isSuccess()) {
                        uploadedCount++;
                        log.info("→ SUCCESS: '{}' created (row {})", productName, rowNumber);
                    } else {
                        skippedCount++;
                        addSkip(skippedReasons, rowNumber,
                                productName + " — " + result.getErrorMessage());
                        log.warn("→ SKIPPED: '{}' (row {}) — {}", productName, rowNumber, result.getErrorMessage());
                    }

                } catch (Exception e) {
                    skippedCount++;
                    String rawName = str(row, col, "product name");
                    addSkip(skippedReasons, rowNumber,
                            (rawName.isEmpty() ? "Unknown product" : rawName)
                                    + " — " + cleanError(e));
                    log.error("→ ERROR row {}: {}", rowNumber, e.getMessage(), e);
                }

            } // end row loop

        } catch (Exception e) {
            String msg = "Cannot read the uploaded Excel file: "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                    + ". Please make sure it is a valid .xlsx file and try again.";
            skippedReasons.add(msg);
            response.setSkippedReasons(skippedReasons);
            response.setMessage("Upload failed — could not read Excel file");
            log.error("CRITICAL: bulk upload aborted", e);
            return response;
        }

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  BULK UPLOAD FINISHED → uploaded: {}, skipped: {}", uploadedCount, skippedCount);
        log.info("╚══════════════════════════════════════════════════════════════╝");

        response.setUploadedCount(uploadedCount);
        response.setSkippedCount(skippedCount);
        response.setSkippedReasons(skippedReasons);

        if (uploadedCount == 0 && skippedCount > 0) {
            response.setMessage("No products were uploaded — please review the issues listed below");
        } else if (skippedCount > 0) {
            response.setMessage("Upload completed — " + uploadedCount + " product(s) added, " + skippedCount + " skipped");
        } else {
            response.setMessage("All " + uploadedCount + " product(s) uploaded successfully");
        }

        return response;
    }
    // ═══════════════════════════════════════════════════════════════════════
    //  FILE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Map<String, MultipartFile> buildFileMap(List<MultipartFile> files) {
        Map<String, MultipartFile> map = new HashMap<>();
        if (files == null || files.isEmpty()) {
            log.info("No files provided in this bulk request");
            return map;
        }
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name != null && !name.trim().isEmpty()) {
                map.put(normalizeKey(name), file);
            }
        }
        log.info("Prepared {} file(s) for lookup", map.size());
        return map;
    }

    private byte[] resolveBytes(Map<String, MultipartFile> fileMap, String filename,
                                String label, String productName, int rowNumber) {
        if (filename == null || filename.trim().isEmpty()) return null;
        MultipartFile file = fileMap.get(normalizeKey(filename));
        if (file == null) {
            log.warn("Row {} → {} not found in upload: '{}' for '{}'", rowNumber, label, filename, productName);
            return null;
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.warn("Row {} → Could not read {} '{}' for '{}': {}", rowNumber, label, filename, productName, e.getMessage());
            return null;
        }
    }

    private List<byte[]> resolveBytesListBySemicolon(Map<String, MultipartFile> fileMap,
                                                     List<String> names, String label,
                                                     String productName, int rowNumber) {
        List<byte[]> result = new ArrayList<>();
        for (String name : names) {
            byte[] b = resolveBytes(fileMap, name, label, productName, rowNumber);
            if (b != null) result.add(b);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  COLUMN / ROW HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Map<String, Integer> buildColumnIndex(Row headerRow) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String h = getCellStr(headerRow.getCell(i))
                    .trim().toLowerCase().replaceAll("\\s+", " ").replaceAll("[^a-z0-9 ]", "");
            if (!h.isEmpty()) colIndex.put(h, i);
        }
        return colIndex;
    }

    private boolean isRowBlank(Row row, Map<String, Integer> col) {
        return col.values().stream()
                .allMatch(idx -> getCellStr(row.getCell(idx)).trim().isEmpty());
    }

    private String str(Row row, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        return idx == null ? "" : getCellStr(row.getCell(idx)).trim();
    }

    private int intVal(Row row, Map<String, Integer> col, String key, int def) {
        String v = str(row, col, key).replaceAll("[^0-9-]", "");
        if (v.isEmpty()) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    private double dbl(Row row, Map<String, Integer> col, String key, double def) {
        String v = str(row, col, key).replaceAll("[^0-9.-]", "");
        if (v.isEmpty()) return def;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return def; }
    }

    private boolean bool(Row row, Map<String, Integer> col, String key, boolean def) {
        Integer idx = col.get(key);
        if (idx == null) return def;
        Cell cell = row.getCell(idx);
        if (cell == null) return def;
        try {
            switch (cell.getCellType()) {
                case BOOLEAN: return cell.getBooleanCellValue();
                case NUMERIC: return cell.getNumericCellValue() == 1.0;
                case STRING: {
                    String s = cell.getStringCellValue().trim().toLowerCase();
                    if (s.isEmpty()) return def;
                    return "true".equals(s) || "yes".equals(s) || "1".equals(s);
                }
                default: return def;
            }
        } catch (Exception e) { return def; }
    }

    private List<String> semicolonList(Row row, Map<String, Integer> col, String key) {
        String val = str(row, col, key);
        if (val.isEmpty()) return new ArrayList<>();
        return Arrays.stream(val.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String getCellStr(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:  return cell.getStringCellValue().trim();
                case NUMERIC: {
                    double d = cell.getNumericCellValue();
                    return (d == Math.floor(d) && !Double.isInfinite(d))
                            ? String.valueOf((long) d) : String.valueOf(d);
                }
                case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
                case FORMULA: return cell.getCellFormula();
                default:      return "";
            }
        } catch (Exception e) { return ""; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  JSON MAP PARSER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Parse a raw JSON object string from an Excel cell into Map<String, String>.
     *
     * Example cell value:  {"material":"Acrylic","weight":"250g"}
     * Returns:             Map {material=Acrylic, weight=250g}
     *
     * Blank cell   → returns null  (field is optional, no error)
     * Invalid JSON → warns + returns null  (row is NOT skipped)
     *
     * Works for: specifications, additionalInfo, faq, customFields
     * which are all Map<String, String> in CreateProductRequestDto.
     */
    private Map<String, String> parseJsonMap(String raw, String fieldName, String productName) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return objectMapper.readValue(raw.trim(),
                    new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Invalid JSON map in '{}' for '{}' — field skipped. Raw value: {}",
                    fieldName, productName, raw);
            return null;
        }
    }


    /**
     * Parses a JSON array string from an Excel cell into List<Object>.
     * Used for customFields which is a list of field descriptor objects.
     * Returns null (not empty list) on blank/invalid so DTO stays clean.
     */
    private List<Object> parseJsonList(String raw, String fieldName, String productName) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Object>>() {});
        } catch (Exception ex) {
            log.warn("Invalid JSON for '{}' on product '{}' — skipping field. Value: {}",
                    fieldName, productName, raw);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MISC HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private String normalizeKey(String name) {
        if (name == null) return "";
        return name.trim()
                .toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("_", "-")
                .replaceAll("-+", "-")
                .replaceFirst("\\.[^.]*$", ""); // strip extension
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private String validatedJsonOrNull(String raw, String fieldName, String productName) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            objectMapper.readTree(raw);
            return raw.trim();
        } catch (Exception e) {
            log.warn("Invalid JSON in '{}' for '{}' — field skipped. Raw: {}", fieldName, productName, raw);
            return null;
        }
    }

    private String safeGet(List<String> list, int index, String fallback) {
        if (list == null || index >= list.size()) return fallback;
        String val = list.get(index);
        return (val == null || val.trim().isEmpty()) ? fallback : val.trim();
    }

    private LocalDate parseLocalDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim()); // expects YYYY-MM-DD
        } catch (Exception e) {
            log.warn("Invalid date format '{}' — expected YYYY-MM-DD, field skipped", s);
            return null;
        }
    }

    private double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(s.replaceAll("[^0-9.-]", "")); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try { return Integer.parseInt(s.replaceAll("[^0-9-]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private String cleanError(Exception e) {
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return e.getMessage();
        }
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return "Unexpected error (" + e.getClass().getSimpleName() + ")";
        }
        return msg.length() > 200 ? msg.substring(0, 200) + "…" : msg;
    }

    private void addSkip(List<String> reasons, int rowNumber, String detail) {
        reasons.add("Row " + rowNumber + ": " + detail);
    }

    private BulkUploadResponse failResponse(BulkUploadResponse response,
                                            List<String> reasons, String msg) {
        reasons.add(msg);
        response.setSkippedReasons(reasons);
        response.setMessage("Upload failed — " + msg);
        log.error("Bulk upload aborted: {}", msg);
        return response;
    }



//─────────────────────────────────────────────────────────────
//   Private helper — maps ProductEntity → ProductCategoryResponse
// ─────────────────────────────────────────────────────────────
    private ProductCategoryResponse mapToCategoryResponse(ProductEntity e) {
        ProductCategoryResponse dto = new ProductCategoryResponse();

        dto.setProductPrimeId(e.getProductPrimeId());
        dto.setProductStrId(e.getProductStrId());
        dto.setProductName(e.getProductName());
        dto.setBrandName(e.getBrandName());
        dto.setProductCategory(e.getProductCategory());
        dto.setProductSubCategory(e.getProductSubCategory());

        dto.setCurrentSku(e.getCurrentSku());
        dto.setSelectedColor(e.getSelectedColor());
        dto.setCurrentSellingPrice(e.getCurrentSellingPrice());
        dto.setCurrentMrpPrice(e.getCurrentMrpPrice());
        dto.setCurrentStock(e.getCurrentStock());

        dto.setIsCustomizable(e.getIsCustomizable());
        dto.setIsDeleted(e.getIsDeleted());
        dto.setIsUnderTrendCategory(e.getUnderTrendCategory());

        // Main image URL (same pattern as your existing mapToResponseDto)
        if (e.getMainImageData() != null && e.getMainImageData().length > 0) {
            dto.setMainImage("/api/products/" + e.getProductPrimeId() + "/main");
        }

        // globalTags
        if (e.getGlobalTags() != null && !"[]".equals(e.getGlobalTags())) {
            try {
                List<String> tags = objectMapper.readValue(
                        e.getGlobalTags(), new TypeReference<List<String>>() {});
                dto.setGlobalTags(tags);
            } catch (Exception ex) {
                log.error("Failed to deserialize globalTags for product {}", e.getProductPrimeId(), ex);
                dto.setGlobalTags(List.of());
            }
        } else {
            dto.setGlobalTags(List.of());
        }

        // addonKeys
        if (e.getAddonKeys() != null && !"[]".equals(e.getAddonKeys())) {
            try {
                List<String> addons = objectMapper.readValue(
                        e.getAddonKeys(), new TypeReference<List<String>>() {});
                dto.setAddonKeys(addons);
            } catch (Exception ex) {
                log.error("Failed to deserialize addonKeys for product {}", e.getProductPrimeId(), ex);
                dto.setAddonKeys(List.of());
            }
        } else {
            dto.setAddonKeys(List.of());
        }

        return dto;
    }

    // ─────────────────────────────────────────────────────────────
    //   Private helper — builds Pageable (reuse your existing sort logic)
    // ─────────────────────────────────────────────────────────────
    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = (sortBy != null && !sortBy.isBlank())
                ? (sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending())
                : Sort.by("productPrimeId").descending();
        return PageRequest.of(page, size, sort);
    }

    // ─────────────────────────────────────────────────────────────
    //   get-by-category
    // ─────────────────────────────────────────────────────────────
    @Override
    public Page<ProductCategoryResponse> getProductsByCategory(
            String category, int page, int size, String sortBy, String sortDir) {

        log.info("Fetching products by category: {}", category);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return productRepository.findByCategory(category, pageable)
                .map(this::mapToCategoryResponse);
    }

    // ─────────────────────────────────────────────────────────────
    //   get-by-sub-category
    // ─────────────────────────────────────────────────────────────
    @Override
    public Page<ProductCategoryResponse> getProductsBySubCategory(
            String subCategory, int page, int size, String sortBy, String sortDir) {

        log.info("Fetching products by subCategory: {}", subCategory);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return productRepository.findBySubCategory(subCategory, pageable)
                .map(this::mapToCategoryResponse);
    }

    // ─────────────────────────────────────────────────────────────
    //   get-by-addon
    // ─────────────────────────────────────────────────────────────
    @Override
    public Page<ProductCategoryResponse> getProductsByAddonKey(
        String addonKey, int page, int size, String sortBy, String sortDir) {

        log.info("Fetching products by addonKey: {}", addonKey);
        Pageable pageable = PageRequest.of(page, size); // ← no sort here
        return productRepository.findByAddonKey(addonKey, pageable)
                .map(this::mapToCategoryResponse);
    }

    // ─────────────────────────────────────────────────────────────
    //   get-by-glob-tag
    // ─────────────────────────────────────────────────────────────

    @Override
    public Page<ProductCategoryResponse> getProductsByGlobalTag(
            String tag, int page, int size, String sortBy, String sortDir) {

        log.info("Fetching products by globalTag: {}", tag);
        Pageable pageable = PageRequest.of(page, size); // ← no sort here
        return productRepository.findByGlobalTag(tag, pageable)
                .map(this::mapToCategoryResponse);
    }

    //-------------------------------------------------------------//
    //                  searching product                          //
    //-------------------------------------------------------------//
    @Override
    public List<ProductSearchResultDto> searchProducts(String keyword, int limit) {
        if (keyword == null || keyword.trim().length() < 2) {
            return Collections.emptyList();
        }
        String sanitized = keyword.trim();
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.searchProducts(sanitized, pageable);
    }


    // ========== NEW METHOD ADDED - Exchange/Return ke liye single variant details ==========
    @Override
    public Map<String, Object> getVariantExchangeDetails(Long productPrimeId, String variantId) {
        ProductEntity product = productRepository.findById(productPrimeId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productPrimeId));

        ProductVariantEntity variant = product.getVariants().stream()
                .filter(v -> variantId.equals(v.getVariantId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + variantId));

        Map<String, Object> details = new HashMap<>();
        details.put("productPrimeId", product.getProductPrimeId());
        details.put("productStrId", product.getProductStrId());
        details.put("productName", product.getProductName());
        details.put("variantId", variant.getVariantId());
        details.put("color", variant.getColor());
        details.put("size", variant.getSize());
        details.put("weight", variant.getWeight());
        details.put("length", variant.getLength());
        details.put("breadth", variant.getBreadth());
        details.put("height", variant.getHeight());
        details.put("isExchangeAvailable", product.getIsExchange());
        details.put("returnAvailable", product.getReturnAvailable());

        return details;
    }
// ========== END OF NEW METHOD ==========

    // ========== NEW METHOD ADDED - Saare variants ki dimensions list ==========
    @Override
    public List<Map<String, Object>> getAllVariantsDimensions(Long productPrimeId) {
        ProductEntity product = productRepository.findById(productPrimeId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productPrimeId));

        List<Map<String, Object>> dimensionsList = new ArrayList<>();

        for (ProductVariantEntity variant : product.getVariants()) {
            Map<String, Object> dim = new HashMap<>();
            dim.put("variantId", variant.getVariantId());
            dim.put("color", variant.getColor());
            dim.put("size", variant.getSize());
            dim.put("length", variant.getLength());
            dim.put("breadth", variant.getBreadth());
            dim.put("height", variant.getHeight());
            dim.put("weight", variant.getWeight());

            // Null safe dimension text
            String dimText = (variant.getLength() != null ? variant.getLength() : "?") + "×" +
                    (variant.getBreadth() != null ? variant.getBreadth() : "?") + "×" +
                    (variant.getHeight() != null ? variant.getHeight() : "?") + " cm";
            dim.put("dimensionText", dimText);

            String weightText = (variant.getWeight() != null ? variant.getWeight() : "?") + " g";
            dim.put("weightText", weightText);

            dimensionsList.add(dim);
        }

        return dimensionsList;
    }
// ========== END OF NEW METHOD ==========
}