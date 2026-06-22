package com.artezo.controller;

import com.artezo.dto.request.AddCustomizedToCartRequest;
import com.artezo.dto.response.CartResponse;
import com.artezo.dto.response.CustomizationUploadResponse;
import com.artezo.exceptions.ApiResponse;
import com.artezo.service.CustomizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/customize")
public class CustomizationController {

    private static final Logger log = LoggerFactory.getLogger(CustomizationController.class);
    private final CustomizationService customizationService;

    public CustomizationController(CustomizationService customizationService) {
        this.customizationService = customizationService;
    }

    /**
     * STEP 1 of customization flow.
     * Upload image — returns assetUuid for subsequent cart add.
     * POST /api/v1/customize/upload-image
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomizationUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "sessionId", required = false) String sessionId) {

        log.info("[CUSTOMIZE] Upload image | userId={}, sessionId={}", userId, sessionId);
        try {
            CustomizationUploadResponse response =
                    customizationService.uploadCustomizationImage(file, userId, sessionId);
            return ResponseEntity.ok(
                    ApiResponse.success("Image uploaded successfully", response));
        } catch (RuntimeException ex) {
            log.error("[CUSTOMIZE] Upload failed | error={}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Upload failed: " + ex.getMessage()));
        }
    }

    /**
     * STEP 2 of customization flow.
     * Add customized product to cart using assetUuid from step 1.
     * POST /api/v1/cart/add-customized  ← note: under /cart prefix for logical grouping
     *
     * Existing POST /api/v1/cart/add is NOT touched.
     */
    @PostMapping(value = "/add-to-cart")
    public ResponseEntity<ApiResponse<CartResponse>> addCustomizedToCart(
            @RequestBody AddCustomizedToCartRequest request) {

        log.info("[CUSTOMIZE] Add customized to cart | userId={}, productId={}, slots={}",
                request.getUserId(), request.getProductId(),
                request.getAssetSlots() != null ? request.getAssetSlots().size() : 0);
        try {
            CartResponse cart = customizationService.addCustomizedToCart(request);
            return ResponseEntity.ok(
                    ApiResponse.success("Customized item added to cart", cart));
        } catch (RuntimeException ex) {
            log.error("[CUSTOMIZE] Add to cart failed | error={}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to add to cart: " + ex.getMessage()));
        }
    }


    /**
     * Serve the customization image — used by cart preview, admin panel, order detail.
     * GET /api/v1/customize/image/{assetUuid}
     */
    @GetMapping("/image/{assetUuid}")
    public ResponseEntity<byte[]> getCustomizationImage(@PathVariable String assetUuid) {
        log.info("[CUSTOMIZE] Serve image | assetUuid={}", assetUuid);
        try {
            byte[] imageBytes = customizationService.getCustomizationImage(assetUuid);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // works for PNG/WEBP too in browsers
                    .body(imageBytes);
        } catch (RuntimeException ex) {
            log.warn("[CUSTOMIZE] Image not found | assetUuid={}", assetUuid);
            return ResponseEntity.notFound().build();
        }
    }
}