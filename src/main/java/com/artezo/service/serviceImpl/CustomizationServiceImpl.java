package com.artezo.service.serviceImpl;

import com.artezo.dto.request.AddCustomizedToCartRequest;
import com.artezo.dto.request.AddToCartRequest;
import com.artezo.dto.response.CartResponse;
import com.artezo.dto.response.CustomizationUploadResponse;
import com.artezo.entity.CartItemCustomizationAssetEntity;
import com.artezo.entity.CartItemEntity;
import com.artezo.entity.CustomizationAssetEntity;

import com.artezo.repository.*;
import com.artezo.service.CustomizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CustomizationServiceImpl implements CustomizationService {

    private static final Logger log = LoggerFactory.getLogger(CustomizationServiceImpl.class);

    // ── Config ────────────────────────────────────────────────────────────────
    // LOCAL DEV:  customized_img  (folder in project root)
    // PRODUCTION: reconfigure in application.properties to absolute VPS path
    @Value("${artezo.customization.upload-dir:customized_img}")
    private String uploadDir;

    @Value("${artezo.customization.base-url:/api/v1/customize/image}")
    private String baseUrl;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final CustomizationAssetRepository assetRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartServiceImpl cartServiceImpl; // reuse existing buildCartResponse

    // ── ADD these repo injections to CustomizationServiceImpl ─────────────────
    private final CartItemCustomizationAssetRepository cartItemAssetRepository;
    private final OrderItemCustomizationAssetRepository orderItemAssetRepository;

    public CustomizationServiceImpl(CustomizationAssetRepository assetRepository, CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository, CartServiceImpl cartServiceImpl, CartItemCustomizationAssetRepository cartItemAssetRepository, OrderItemCustomizationAssetRepository orderItemAssetRepository) {
        this.assetRepository = assetRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.cartServiceImpl = cartServiceImpl;
        this.cartItemAssetRepository = cartItemAssetRepository;
        this.orderItemAssetRepository = orderItemAssetRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. UPLOAD IMAGE
    // POST /api/v1/customize/upload-image
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public CustomizationUploadResponse uploadCustomizationImage(MultipartFile file,
                                                                Long userId,
                                                                String sessionId) {
        log.info("[CUSTOMIZE] Upload request | userId={}, sessionId={}, fileName={}",
                userId, sessionId, file.getOriginalFilename());

        // ── Validate ──────────────────────────────────────────────────────────
        if (file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Only JPEG, PNG, WEBP images are allowed");
        }

        // ── Build stored filename ─────────────────────────────────────────────
        String assetUuid = UUID.randomUUID().toString();
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storedFilename = assetUuid + "_" + originalFilename;

        // ── Ensure directory exists ───────────────────────────────────────────
        // LOCAL: creates  ./customized_img/  relative to working directory
        // VPS:   will point to absolute path via application.properties override
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[CUSTOMIZE] File saved to disk | path={}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[CUSTOMIZE] Failed to save file | error={}", e.getMessage());
            throw new RuntimeException("Failed to save customization image: " + e.getMessage());
        }

        // ── Persist asset record ──────────────────────────────────────────────
        CustomizationAssetEntity asset = new CustomizationAssetEntity();
        asset.setAssetUuid(assetUuid);
        asset.setUserId(userId);
        asset.setSessionId(sessionId);
        asset.setOriginalFilename(originalFilename);
        asset.setStoredFilename(storedFilename);
        asset.setFilePath(uploadDir + "/" + storedFilename);
        asset.setFileSizeBytes(file.getSize());
        asset.setMimeType(contentType);
        asset.setStatus(CustomizationAssetEntity.AssetStatus.PENDING);
        asset.setExpiresAt(LocalDateTime.now().plusDays(7)); // GC after 7d if never ordered

        assetRepository.save(asset);
        log.info("[CUSTOMIZE] Asset record saved | assetUuid={}", assetUuid);

        String previewUrl = baseUrl + "/" + assetUuid;

        return new CustomizationUploadResponse(
                assetUuid,
                previewUrl,
                originalFilename,
                file.getSize()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. ADD CUSTOMIZED PRODUCT TO CART
    // POST /api/v1/cart/add-customized
    // Existing /api/v1/cart/add is NOT touched
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public CartResponse addCustomizedToCart(AddCustomizedToCartRequest request) {
        log.info("[CUSTOMIZE] Add customized to cart | userId={}, productId={}, slots={}",
                request.getUserId(), request.getProductId(),
                request.getAssetSlots() != null ? request.getAssetSlots().size() : 0);

        List<AddCustomizedToCartRequest.AssetSlotRequest> slots =
                request.getAssetSlots();

        // ── Validate all asset UUIDs exist ────────────────────────────────────
        List<CustomizationAssetEntity> resolvedAssets = new ArrayList<>();
        if (slots != null && !slots.isEmpty()) {
            for (AddCustomizedToCartRequest.AssetSlotRequest slot : slots) {
                CustomizationAssetEntity asset = assetRepository
                        .findByAssetUuid(slot.getAssetUuid())
                        .orElseThrow(() -> new RuntimeException(
                                "Asset not found: " + slot.getAssetUuid()));

                // Security: asset must belong to this user
                if (request.getUserId() != null && asset.getUserId() != null
                        && !asset.getUserId().equals(request.getUserId())) {
                    throw new RuntimeException(
                            "Asset does not belong to this user: " + slot.getAssetUuid());
                }
                resolvedAssets.add(asset);
            }
        }

        // ── Primary asset = slot 1 ────────────────────────────────────────────
        CustomizationAssetEntity primaryAsset =
                resolvedAssets.isEmpty() ? null : resolvedAssets.get(0);

        // ── Add to cart via existing CartServiceImpl (untouched) ─────────────
        AddToCartRequest normalRequest = toNormalRequest(request);
        CartResponse cartResponse = cartServiceImpl.addToCart(normalRequest);

        // ── Find the CartItem just saved ──────────────────────────────────────
        Long cartId = cartResponse.getCartId();
        CartItemEntity cartItem = cartItemRepository
                .findByCart_IdAndProductIdAndVariantId(
                        cartId, request.getProductId(), request.getVariantId())
                .orElseThrow(() -> new RuntimeException(
                        "CartItem not found after add"));

        // ── Clear old assets (re-add flow — same product customized again) ────
        cartItemAssetRepository.deleteAllByCartItemId(cartItem.getId());

        // ── Set primary asset FK on CartItem ──────────────────────────────────
        if (primaryAsset != null) {
            cartItem.setCustomizationAsset(primaryAsset);
            // Override productImageUrl → custom image URL for cart preview
            cartItem.setProductImageUrl(
                    baseUrl + "/" + primaryAsset.getAssetUuid());
        }
        cartItemRepository.save(cartItem);

        // ── Save all slots to join table ──────────────────────────────────────
        if (slots != null) {
            for (int i = 0; i < slots.size(); i++) {
                AddCustomizedToCartRequest.AssetSlotRequest slotReq = slots.get(i);
                CustomizationAssetEntity asset = resolvedAssets.get(i);

                CartItemCustomizationAssetEntity slotEntity =
                        new CartItemCustomizationAssetEntity();
                slotEntity.setCartItem(cartItem);
                slotEntity.setAsset(asset);
                slotEntity.setSlotNumber(slotReq.getSlotNumber());
                slotEntity.setFieldName(slotReq.getFieldName());
                cartItemAssetRepository.save(slotEntity);

                // Update all assets status → IN_CART
                asset.setStatus(CustomizationAssetEntity.AssetStatus.IN_CART);
                asset.setExpiresAt(null);
                assetRepository.save(asset);

                log.info("[CUSTOMIZE] Slot {} saved | cartItemId={}, assetUuid={}",
                        slotReq.getSlotNumber(), cartItem.getId(),
                        slotReq.getAssetUuid());
            }
        }

        // ── Return fresh cart ─────────────────────────────────────────────────
        return cartServiceImpl.getCart(request.getUserId());
    }
    // ─────────────────────────────────────────────────────────────────────────
    // 3. SERVE CUSTOMIZATION IMAGE
    // GET /api/v1/customize/image/{assetUuid}
    // Used by: cart page preview, admin order panel, order confirmation
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public byte[] getCustomizationImage(String assetUuid) {
        log.info("[CUSTOMIZE] Serving image | assetUuid={}", assetUuid);

        CustomizationAssetEntity asset = assetRepository
                .findByAssetUuid(assetUuid)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetUuid));

        Path filePath = Paths.get(asset.getFilePath());
        if (!Files.exists(filePath)) {
            log.error("[CUSTOMIZE] File not found on disk | path={}", filePath);
            throw new RuntimeException("Image file not found on disk");
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. GC SCHEDULER — runs daily at 3AM
    // Deletes PENDING assets that were never added to cart (abandoned uploads)
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredPendingAssets() {
        log.info("[CUSTOMIZE-GC] Running expired asset cleanup");

        List<CustomizationAssetEntity> expired = assetRepository
                .findByStatusAndExpiresAtBefore(
                        CustomizationAssetEntity.AssetStatus.PENDING,
                        LocalDateTime.now()
                );

        int deletedCount = 0;
        for (CustomizationAssetEntity asset : expired) {
            // Delete physical file
            try {
                Path filePath = Paths.get(asset.getFilePath());
                Files.deleteIfExists(filePath);
                log.info("[CUSTOMIZE-GC] Deleted file | path={}", filePath);
            } catch (IOException e) {
                log.warn("[CUSTOMIZE-GC] Failed to delete file | assetUuid={} | error={}",
                        asset.getAssetUuid(), e.getMessage());
            }
            // Mark as EXPIRED (soft delete — keeps audit trail in DB)
            asset.setStatus(CustomizationAssetEntity.AssetStatus.EXPIRED);
            assetRepository.save(asset);
            deletedCount++;
        }
        log.info("[CUSTOMIZE-GC] Cleanup complete | expiredCount={}", deletedCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    // Converts AddCustomizedToCartRequest → AddToCartRequest
    // so we reuse existing CartServiceImpl.addToCart() without any change
    private AddToCartRequest toNormalRequest(AddCustomizedToCartRequest req) {
        AddToCartRequest r = new AddToCartRequest();
        r.setUserId(req.getUserId());
        r.setSessionId(req.getSessionId());
        r.setProductId(req.getProductId());
        r.setVariantId(req.getVariantId());
        r.setSku(req.getSku());
        r.setSelectedColor(req.getSelectedColor());
        r.setSelectedSize(req.getSelectedSize());
        r.setTitleName(req.getTitleName());
        r.setProductName(req.getProductName());
        r.setUnitPrice(req.getUnitPrice());
        r.setMrpPrice(req.getMrpPrice());
        r.setQuantity(req.getQuantity());
        r.setCustomFieldsJson(req.getCustomFieldsJson());
        return r;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "upload.jpg";
        // Remove path traversal characters, keep only safe chars
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}