package com.artezo.service.serviceImpl;

import com.artezo.dto.request.CreateProductRequestDto;
import com.artezo.dto.request.HeroBannerRequestDto;
import com.artezo.dto.request.InstallationStepRequestDto;
import com.artezo.dto.request.VariantRequestDto;
import com.artezo.dto.response.HeroBannerResponseDto;
import com.artezo.dto.response.InstallationStepResponseDto;
import com.artezo.dto.response.ProductResponseDto;
import com.artezo.dto.response.VariantResponseDto;
import com.artezo.entity.InstallationStepEntity;
import com.artezo.entity.InventoryEntity;
import com.artezo.entity.ProductEntity;
import com.artezo.entity.ProductVariantEntity;
import com.artezo.exceptions.ProductCreateResult;
import com.artezo.repository.InstallationStepRepository;
import com.artezo.repository.InventoryRepository;
import com.artezo.repository.ProductRepository;
import com.artezo.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InstallationStepRepository installationStepRepository;
    private final InventoryRepository inventoryRepository;

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductServiceImpl(
            ProductRepository productRepository,
            InstallationStepRepository installationStepRepository,
            InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.installationStepRepository = installationStepRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // ────────────────────────────────────────────────
    //      URL generators
    // ────────────────────────────────────────────────
    private String mainImageUrl(Long productId) {
        return "/api/products/" + productId + "/main";
    }

    private String mockupImageUrl(Long productId, int index) {
        return "/api/products/" + productId + "/mockup/" + index;
    }

    private String variantMainImageUrl(Long productId, String variantId) {
        return "/api/products/" + productId + "/variant/" + variantId + "/main";
    }

    // ────────────────────────────────────────────────
    //                  CREATE PRODUCT + INVENTORY SYNC
    // ────────────────────────────────────────────────
    @Override
    @Transactional
    public ProductCreateResult createProduct(CreateProductRequestDto request) {
        log.info("Creating product → name: {}, hasVariants: {}",
                request.getProductName(), request.isHasVariants());

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
        if (Boolean.TRUE.equals(request.isHasVariants()) && request.getVariants() != null) {
            for (VariantRequestDto vReq : request.getVariants()) {
                ProductVariantEntity variant = new ProductVariantEntity();
                mapVariantToEntity(variant, vReq);
                variant.setProduct(entity);
                entity.getVariants().add(variant);
            }
        }

        // Generate productStrId
        long count = productRepository.count() + 1;
        entity.setProductStrId(String.format("PRD%05d", count));

        ProductEntity saved = productRepository.save(entity);
        log.info("Product created → id: {}, strId: {}", saved.getProductPrimeId(), saved.getProductStrId());

        // ─── AUTO-GENERATE variantIds AFTER save ───
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

        // Save installation steps
        saveInstallationSteps(saved, request);

        // INVENTORY SYNC
        syncInventoryFromProduct(saved);

        ProductResponseDto responseDto = mapToResponseDto(saved);
        return ProductCreateResult.success(responseDto);
    }

    // Get product video
    @Cacheable(value = "productVideos", key = "#productId")
    @Override
    public byte[] getProductVideoData(Long productId) {
        return productRepository.findById(productId)
                .map(ProductEntity::getProductVideoData)
                .orElse(null);
    }


    // ────────────────────────────────────────────────
    //                  UPDATE FULL + INVENTORY SYNC
    // ────────────────────────────────────────────────
    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, CreateProductRequestDto request) {
        log.info("Full update product id: {}", id);

        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        mapBaseFieldsToEntity(entity, request);

        // Replace images
        entity.setMainImageData(request.getMainImage());
        entity.getMockupImageDataList().clear();
        if (request.getMockupImages() != null) {
            entity.getMockupImageDataList().addAll(request.getMockupImages());
        }

        // Replace variants → regenerate IDs after save
        entity.getVariants().clear();
        if (Boolean.TRUE.equals(request.isHasVariants()) && request.getVariants() != null) {
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
    @Override
    @Transactional
    public ProductResponseDto patchProduct(Long id, CreateProductRequestDto request) {
        log.info("Partial patch product id: {}", id);

        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (request.getProductName() != null) entity.setProductName(request.getProductName());
        if (request.getBrandName() != null) entity.setBrandName(request.getBrandName());
        if (request.getProductCategory() != null) entity.setProductCategory(request.getProductCategory());
        if (request.getProductSubCategory() != null) entity.setProductSubCategory(request.getProductSubCategory());

        if (request.getCurrentSku() != null) entity.setCurrentSku(request.getCurrentSku());
        if (request.getSelectedColor() != null) entity.setSelectedColor(request.getSelectedColor());
        if (request.getCurrentSellingPrice() != null) entity.setCurrentSellingPrice(request.getCurrentSellingPrice());
        if (request.getCurrentMrpPrice() != null) entity.setCurrentMrpPrice(request.getCurrentMrpPrice());
        if (request.getCurrentStock() != null) entity.setCurrentStock(request.getCurrentStock());

        if (request.getMainImage() != null) entity.setMainImageData(request.getMainImage());
        if (request.getMockupImages() != null) {
            entity.getMockupImageDataList().clear();
            entity.getMockupImageDataList().addAll(request.getMockupImages());
        }

        if (request.getHeroBanners() != null) {
            try {
                String json = objectMapper.writeValueAsString(request.getHeroBanners());
                entity.setHeroBanners(json);
            } catch (Exception ex) {
                log.error("Patch hero banners failed", ex);
            }
        }

        // Partial update for installation steps (only if provided)
        if (request.getInstallationSteps() != null) {
            installationStepRepository.deleteByProduct_ProductPrimeId(entity.getProductPrimeId());
            saveInstallationSteps(entity, request);
        }

        // ─── Variants in patch: full replace if provided ───
        if (request.getVariants() != null) {
            entity.getVariants().clear();
            for (VariantRequestDto vReq : request.getVariants()) {
                ProductVariantEntity variant = new ProductVariantEntity();
                mapVariantToEntity(variant, vReq);
                variant.setProduct(entity);
                entity.getVariants().add(variant);
            }
        }

        ProductEntity saved = productRepository.save(entity);

        // Regenerate variantIds if variants were changed or exist
        if ((request.getVariants() != null || !entity.getVariants().isEmpty()) && saved.getVariants() != null) {
            for (ProductVariantEntity variant : saved.getVariants()) {
                String color = variant.getColor() != null ? variant.getColor().trim() : "";
                String colorPart = color.isEmpty() ? "DEFAULT" :
                        color.toUpperCase().replaceAll("[^A-Z0-9]", "");

                String generatedId = String.format("VAR-%s-%d", colorPart, variant.getId());
                variant.setVariantId(generatedId);
            }
            saved = productRepository.save(saved);
        }

        if (request.getCurrentStock() != null || request.getVariants() != null) {
            syncInventoryFromProduct(saved);
        }

        return mapToResponseDto(saved);
    }

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
    //      Other methods
    // ────────────────────────────────────────────────
    @Override
    public ProductResponseDto getProductById(Long id) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponseDto(entity);
    }

    @Override
    public ProductResponseDto getProductByStrId(String productStrId) {
        ProductEntity entity = productRepository.findByProductStrId(productStrId)
                .orElseThrow(() -> new RuntimeException("Product not found with strId: " + productStrId));
        return mapToResponseDto(entity);
    }

    @Override
    public void deleteProduct(Long id) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        entity.setIsDeleted(true);
        productRepository.save(entity);
    }

    @Override
    public byte[] getProductMainImageData(Long productId) {
        return productRepository.findById(productId)
                .map(ProductEntity::getMainImageData)
                .orElse(null);
    }

    @Override
    public List<byte[]> getProductMockupImagesData(Long productId) {
        return productRepository.findById(productId)
                .map(ProductEntity::getMockupImageDataList)
                .orElse(List.of());
    }

    @Override
    public byte[] getVariantMainImageData(Long productId, String variantId) {
        return productRepository.findById(productId)
                .flatMap(p -> p.getVariants().stream()
                        .filter(v -> variantId.equals(v.getVariantId()))
                        .findFirst()
                        .map(ProductVariantEntity::getMainImageData))
                .orElse(null);
    }


    // ────────────────────────────────────────────────
    //              Get Product Video
    // ────────────────────────────────────────────────
    @Cacheable(value = "installationVideos", key = "#productId + '-' + #stepIndex")
    @Override
    public byte[] getInstallationVideoData(Long productId, int stepIndex) {
        InstallationStepEntity step = installationStepRepository
                .findByProduct_ProductPrimeIdAndStep(productId, stepIndex);
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
        e.setHasVariants(r.isHasVariants());
        e.setIsCustomizable(r.getIsCustomizable());
        e.setIsExchange(r.getIsExchange());

        e.setCurrentSku(r.getCurrentSku());
        e.setSelectedColor(r.getSelectedColor());
        e.setCurrentSellingPrice(r.getCurrentSellingPrice());
        e.setCurrentMrpPrice(r.getCurrentMrpPrice());
        e.setCurrentStock(r.getCurrentStock());

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
        v.setMainImageData(r.getMainImage());
        v.setMfgDate(r.getMfgDate());
        v.setExpDate(r.getExpDate());
        v.setSize(r.getSize());
        // variantId is NOT set here anymore — generated later
    }

    private ProductResponseDto mapToResponseDto(ProductEntity e) {
        ProductResponseDto dto = new ProductResponseDto();

        dto.setProductId(e.getProductPrimeId());
        dto.setProductStrId(e.getProductStrId());
        dto.setProductName(e.getProductName());
        dto.setBrandName(e.getBrandName());
        dto.setProductCategory(e.getProductCategory());
        dto.setProductSubCategory(e.getProductSubCategory());

        dto.setIsDeleted(e.getIsDeleted());
        dto.setHasVariants(e.isHasVariants());
        dto.setIsCustomizable(e.getIsCustomizable());
        dto.setIsExchange(e.getIsExchange());

        dto.setCurrentSku(e.getCurrentSku());
        dto.setSelectedColor(e.getSelectedColor());
        dto.setCurrentSellingPrice(e.getCurrentSellingPrice());
        dto.setCurrentMrpPrice(e.getCurrentMrpPrice());
        dto.setCurrentStock(e.getCurrentStock());
        dto.setYoutubeUrl(e.getYoutubeUrl());
        dto.setReturnAvailable(e.getReturnAvailable());

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

        if (e.isHasVariants() && e.getVariants() != null && !e.getVariants().isEmpty()) {
            List<VariantResponseDto> variantsDto = e.getVariants().stream()
                    .map(v -> {
                        VariantResponseDto vd = new VariantResponseDto();
                        vd.setVariantId(v.getVariantId());  // now auto-generated
                        vd.setTitleName(v.getTitleName());
                        vd.setColor(v.getColor());
                        vd.setSku(v.getSku());
                        vd.setPrice(v.getPrice());
                        vd.setMrp(v.getMrp());
                        vd.setStock(v.getStock());

                        if (v.getMainImageData() != null && v.getMainImageData().length > 0) {
                            vd.setMainImage(variantMainImageUrl(e.getProductPrimeId(), v.getVariantId()));
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

}