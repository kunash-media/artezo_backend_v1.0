package com.artezo.controller;

import com.artezo.dto.request.CreateProductRequestDto;
import com.artezo.dto.request.HeroBannerRequestDto;
import com.artezo.dto.request.InstallationStepRequestDto;
import com.artezo.dto.response.BulkUploadResponse;
import com.artezo.dto.response.ProductCategoryResponse;
import com.artezo.dto.response.ProductResponseDto;
import com.artezo.exceptions.ProductAlreadyDeletedException;
import com.artezo.exceptions.ProductCreateResult;
import com.artezo.exceptions.ProductNotFoundException;
import com.artezo.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);


    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ──────────────────────────────────────────────────────────
    //          GET BY productPrimeId API for admin and web view
    // ──────────────────────────────────────────────────────────

    //-------- for admin view product details ---------//
    @GetMapping("/admin/get-by-productPrimeId/{productPrimeId}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long productPrimeId) {
        log.info("Fetching product by productPrimeId: {}", productPrimeId);
        return ResponseEntity.ok(productService.getAdminViewProductById(productPrimeId));
    }

    //------ for web view product details  ------------//
    @GetMapping("/get-by-productPrimeId/{productPrimeId}")
    public ResponseEntity<?> getProductById(
            @PathVariable Long productPrimeId,
            @RequestParam(value = "userId", required = false) Long userId) {

        log.info("GET /get-by-productPrimeId/{} | userId: {}", productPrimeId, userId);

        try {
            ProductResponseDto product = productService.getProductById(productPrimeId, userId);
            return ResponseEntity.ok(product);

        } catch (RuntimeException e) {
            // Check if it's actually a not-found or something else
            boolean isNotFound = e.getMessage() != null && e.getMessage().contains("not found");

            return ResponseEntity.status(isNotFound ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", isNotFound ? "Product not found" : "Internal server error",
                            "message", e.getMessage(),
                            "productPrimeId", productPrimeId
                    ));
        }
    }

    @GetMapping("/get-product-by-productStrId/{productStrId}")
    public ResponseEntity<ProductResponseDto> getProductByStrId(@PathVariable String productStrId) {
        log.info("Fetching product by strId: {}", productStrId);
        return ResponseEntity.ok(productService.getProductByStrId(productStrId));
    }

    @PostMapping(value = "/create-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestPart("productJsonData") CreateProductRequestDto request,
            // Main product images - optional
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "mockupImages", required = false) List<MultipartFile> mockupImages,
            // Variant images - optional (indexed)
            @RequestPart(value = "variantsMainImages", required = false) List<MultipartFile> variantMainImages,
            // Hero banners images - optional (indexed)
            @RequestPart(value = "heroBannersImages", required = false) List<MultipartFile> heroBannerImages,
            // ─── NEW: Main product video (single file, optional) ───
            @RequestPart(value = "productVideo", required = false) MultipartFile productVideo,
            // Installation steps images & videos - optional (indexed)
            @RequestPart(value = "installationStepsImages", required = false) List<MultipartFile> stepImages,
            @RequestPart(value = "installationStepsVideos", required = false) List<MultipartFile> stepVideos) {

        log.info("Creating product: name = {}, hasVariants = {}",
                request.getProductName(), request.getHasVariants());

        // ── Assign main image ──
        if (mainImage != null && !mainImage.isEmpty()) {
            try {
                request.setMainImage(mainImage.getBytes());
            } catch (IOException e) {
                log.warn("Failed to read mainImage bytes", e);
            }
        }

        // ── Assign mockup images ──
        if (mockupImages != null && !mockupImages.isEmpty()) {
            List<byte[]> mockupBytes = new ArrayList<>();
            for (MultipartFile file : mockupImages) {
                if (file != null && !file.isEmpty()) {
                    try {
                        mockupBytes.add(file.getBytes());
                    } catch (IOException e) {
                        log.warn("Failed to read mockup image", e);
                    }
                }
            }
            request.setMockupImages(mockupBytes);
        }

        // ── Assign variant main images (by index) ──
        if (request.getVariants() != null && variantMainImages != null) {
            for (int i = 0; i < Math.min(request.getVariants().size(), variantMainImages.size()); i++) {
                MultipartFile file = variantMainImages.get(i);
                if (file != null && !file.isEmpty()) {
                    try {
                        request.getVariants().get(i).setMainImage(file.getBytes());
                    } catch (IOException e) {
                        log.warn("Failed to read variant main image at index {}", i, e);
                    }
                }
            }
        }

        // ── Assign hero banners images (by index) ──
        if (request.getHeroBanners() != null && heroBannerImages != null) {
            for (int i = 0; i < Math.min(request.getHeroBanners().size(), heroBannerImages.size()); i++) {
                MultipartFile file = heroBannerImages.get(i);
                if (file != null && !file.isEmpty()) {
                    try {
                        request.getHeroBanners().get(i).setBannerImg(file.getBytes());
                    } catch (IOException e) {
                        log.warn("Failed to read hero banner image at index {}", i, e);
                    }
                }
            }
        }

        // ─── NEW: Assign main product video ───
        if (productVideo != null && !productVideo.isEmpty()) {
            try {
                request.setProductVideo(productVideo.getBytes());
            } catch (IOException e) {
                log.warn("Failed to read product video bytes", e);
            }
        }

        // ── Assign installation steps images & videos ──
        if (request.getInstallationSteps() != null) {
            // Step images
            if (stepImages != null) {
                for (int i = 0; i < Math.min(request.getInstallationSteps().size(), stepImages.size()); i++) {
                    MultipartFile file = stepImages.get(i);
                    if (file != null && !file.isEmpty()) {
                        try {
                            request.getInstallationSteps().get(i).setStepImage(file.getBytes());
                        } catch (IOException e) {
                            log.warn("Failed to read installation step image at index {}", i, e);
                        }
                    }
                }
            }

            // Step videos
            if (stepVideos != null) {
                for (int i = 0; i < Math.min(request.getInstallationSteps().size(), stepVideos.size()); i++) {
                    MultipartFile file = stepVideos.get(i);
                    if (file != null && !file.isEmpty()) {
                        try {
                            request.getInstallationSteps().get(i).setVideoFile(file.getBytes());
                        } catch (IOException e) {
                            log.warn("Failed to read installation step video at index {}", i, e);
                        }
                    }
                }
            }
        }

        // ── Call service ──
        ProductCreateResult result = productService.createProduct(request);

        if (result.isSuccess()) {
            return ResponseEntity
                    .status(result.getStatus())
                    .body(result.getDto());
        } else {
            return ResponseEntity
                    .status(result.getStatus())
                    .body(result.getErrorMessage());
        }
    }

    @PutMapping("/put-product/{productPrimeId}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long productPrimeId, @RequestBody CreateProductRequestDto request) {
        log.info("Full update product productPrimeId: {}", productPrimeId);
        return ResponseEntity.ok(productService.updateProduct(productPrimeId, request));
    }


    //-------------------------------------//
    //           PATCH API                 //
    //-------------------------------------//
    @PatchMapping(value = "/patch-product/{productPrimeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDto> patchProduct(
            @PathVariable Long productPrimeId,
            @RequestPart(value = "productJsonData", required = false) CreateProductRequestDto request,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImageFile,
            @RequestPart(value = "mockupImages", required = false) List<MultipartFile> mockupImageFiles,
            @RequestPart(value = "productVideo", required = false) MultipartFile productVideoFile,
            @RequestPart(value = "variantsMainImages", required = false) List<MultipartFile> variantMainImages,
            @RequestPart(value = "heroBannersImages", required = false) List<MultipartFile> heroBannerImages,
            @RequestPart(value = "installationStepsImages", required = false) List<MultipartFile> stepImages,
            @RequestPart(value = "installationStepsVideos", required = false) List<MultipartFile> stepVideos
    ) throws IOException {
        log.info("PATCH product {}", productPrimeId);

        if (request == null) request = new CreateProductRequestDto();

        // ── Core files ──
        if (mainImageFile != null && !mainImageFile.isEmpty()) {
            request.setMainImage(mainImageFile.getBytes());
        }
        if (mockupImageFiles != null && !mockupImageFiles.isEmpty()) {
            List<byte[]> mockups = new ArrayList<>();
            for (MultipartFile f : mockupImageFiles) {
                if (!f.isEmpty()) mockups.add(f.getBytes());
            }
            request.setMockupImages(mockups);
        }
        if (productVideoFile != null && !productVideoFile.isEmpty()) {
            request.setProductVideo(productVideoFile.getBytes());
        }

        // ── Variant images — index based (variants always sent in full) ──
        if (variantMainImages != null && !variantMainImages.isEmpty()
                && request.getVariants() != null) {
            for (int i = 0; i < variantMainImages.size(); i++) {
                MultipartFile f = variantMainImages.get(i);
                if (f != null && !f.isEmpty() && i < request.getVariants().size()) {
                    request.getVariants().get(i).setMainImage(f.getBytes());
                }
            }
        }

        // ── Hero banner images ──
        // ✅ Queue approach: files arrive in same order as banners that had a new file
        // file[0] → first banner in JSON, file[1] → second banner in JSON, etc.
        // ── Hero banner images — file[i] maps to heroBanners[i] that had a file ──
        if (heroBannerImages != null && request.getHeroBanners() != null) {
            Queue<MultipartFile> bannerFileQueue = heroBannerImages.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .collect(Collectors.toCollection(LinkedList::new));

            for (HeroBannerRequestDto banner : request.getHeroBanners()) {
                if (bannerFileQueue.isEmpty()) break;
                banner.setBannerImg(bannerFileQueue.poll().getBytes());
            }
        }

        // ── Step images — match file to step by step number using index marker ──
        if (stepImages != null && request.getInstallationSteps() != null) {
            // Frontend sends file only for steps that have new image
            // We need to know WHICH steps have files — frontend must tell us via a hidden field
            // Solution: frontend sets data-has-img="true" on blocks with files
            // Then only those steps appear first in JSON — queue matches in order
            Queue<MultipartFile> imgQueue = stepImages.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .collect(Collectors.toCollection(LinkedList::new));

            // Only assign to steps that actually have a file marker
            for (InstallationStepRequestDto step : request.getInstallationSteps()) {
                if (imgQueue.isEmpty()) break;
                if (Boolean.TRUE.equals(step.getHasNewImage())) { // ✅ only assign if flagged
                    step.setStepImage(imgQueue.poll().getBytes());
                }
            }
        }

        if (stepVideos != null && request.getInstallationSteps() != null) {
            Queue<MultipartFile> vidQueue = stepVideos.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .collect(Collectors.toCollection(LinkedList::new));

            for (InstallationStepRequestDto step : request.getInstallationSteps()) {
                if (vidQueue.isEmpty()) break;
                if (Boolean.TRUE.equals(step.getHasNewVideo())) {
                    step.setVideoFile(vidQueue.poll().getBytes());
                }
            }
        }

        return ResponseEntity.ok(productService.patchProduct(productPrimeId, request));
    }


    //---------------------------------------//
    //              Delete API               //
    //---------------------------------------//
    @DeleteMapping(value = "/delete-by-productPrimeId/{productPrimeId}",
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteProduct(@PathVariable Long productPrimeId) {
        log.info("Attempting soft delete for productPrimeId: {}", productPrimeId);

        try {
            productService.deleteProduct(productPrimeId);
            String successMsg = "Product with ID " + productPrimeId + " deleted successfully";
            log.info(successMsg);
            return ResponseEntity.ok(successMsg);
        }
        catch (ProductNotFoundException e) {
            String errorMsg = "Product not found with ID: " + productPrimeId;
            log.warn(errorMsg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMsg);
        }
        catch (IllegalStateException e) {
            // Example: thrown when product is already deleted
            String errorMsg = "Product with ID " + productPrimeId + " is already deleted";
            log.warn(errorMsg);
            return ResponseEntity.status(HttpStatus.GONE).body(errorMsg);
            // or use HttpStatus.BAD_REQUEST if you prefer
        }
        catch (Exception e) {
            String errorMsg = "Failed to delete product with ID " + productPrimeId + ": " + e.getMessage();
            log.error("Delete failed for productPrimeId: {}", productPrimeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting the product");
        } catch (ProductAlreadyDeletedException e) {
            throw new RuntimeException(e);
        }
    }

    // ────────────────────────────────────────────────
    //    IMAGE SERVING (byte[] → fast for small files)
    // ────────────────────────────────────────────────
    @GetMapping(value = "/{productPrimeId}/main", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<byte[]> getMainImage(@PathVariable Long productPrimeId) {
        byte[] data = productService.getProductMainImageData(productPrimeId);
        if (data == null || data.length == 0) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // improve detection later
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping(value = "/{productPrimeId}/mockup/{index}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getMockupImage(@PathVariable Long productPrimeId, @PathVariable int index) {
        List<byte[]> images = productService.getProductMockupImagesData(productPrimeId);
        if (images == null || index < 0 || index >= images.size()) return ResponseEntity.notFound().build();

        byte[] data = images.get(index);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping(value = "/{productPrimeId}/variant/{variantId}/main", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getVariantMainImage(@PathVariable Long productPrimeId, @PathVariable String variantId) {
        byte[] data = productService.getVariantMainImageData(productPrimeId, variantId);
        if (data == null || data.length == 0) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(data.length)
                .body(data);
    }


    // Hero Banner Image
    @GetMapping("/{productPrimeId}/hero-banner/{bannerId}")
    public ResponseEntity<byte[]> getHeroBannerImage(
            @PathVariable Long productPrimeId,
            @PathVariable String bannerId) {

        byte[] imageData = productService.getHeroBannerImage(productPrimeId, bannerId);
        if (imageData == null || imageData.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageData);
    }

    // Installation Step Image
    @GetMapping("/{productPrimeId}/step/{step}/image")
    public ResponseEntity<byte[]> getInstallationStepImage(
            @PathVariable Long productPrimeId,
            @PathVariable Integer step) {

        byte[] imageData = productService.getInstallationStepImage(productPrimeId, step);
        if (imageData == null || imageData.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageData);
    }

    // ────────────────────────────────────────────────
    //     VIDEO STREAMING (Range support + URL in DTO)
    // ────────────────────────────────────────────────

    @GetMapping(value = "/{productPrimeId}/installation-video/{stepIndex}", produces = "video/mp4")
    public ResponseEntity<InputStreamResource> streamInstallationVideo(
            @PathVariable Long productPrimeId,
            @PathVariable int stepIndex,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request) throws IOException {

        byte[] fullVideoBytes = productService.getInstallationVideoData(productPrimeId, stepIndex);
        if (fullVideoBytes == null || fullVideoBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        long fileSize = fullVideoBytes.length;
        String etag = "\"" + Integer.toHexString(Arrays.hashCode(fullVideoBytes)) + "\"";

        // ✅ Handle 304 - client already has it cached
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        long start = 0, end = fileSize - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            try {
                if (ranges[0].length() > 0) start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && ranges[1].length() > 0) end = Long.parseLong(ranges[1]);
                if (end >= fileSize) end = fileSize - 1;
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }
        }

        long contentLength = end - start + 1;
        ByteArrayInputStream bis = new ByteArrayInputStream(fullVideoBytes, (int) start, (int) contentLength);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/mp4"));
        headers.setContentLength(contentLength);
        headers.set("Accept-Ranges", "bytes");
        headers.set("ETag", etag);
        headers.set("Cache-Control", "public, max-age=86400"); // ✅ browser caches
        headers.set("Last-Modified", "Wed, 01 Jan 2025 00:00:00 GMT"); // or actual timestamp

        if (rangeHeader != null) {
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            return new ResponseEntity<>(new InputStreamResource(bis), headers, HttpStatus.PARTIAL_CONTENT);
        }

        return new ResponseEntity<>(new InputStreamResource(bis), headers, HttpStatus.OK);
    }


    /**
     * Get the main product video
     * Returns 200 with video bytes or 404 if not found
     */
    @GetMapping(value = "/{productPrimeId}/product-video", produces = {"video/mp4", "application/octet-stream"})
    public ResponseEntity<InputStreamResource> getProductVideo(
            @PathVariable Long productPrimeId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request) throws IOException {

        log.info("Streaming product video for productId {}", productPrimeId);

        byte[] fullVideoBytes = productService.getProductVideoData(productPrimeId);

        if (fullVideoBytes == null || fullVideoBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        long fileSize = fullVideoBytes.length;
        String etag = "\"" + Integer.toHexString(Arrays.hashCode(fullVideoBytes)) + "\"";

        // ✅ 304 - client already has it cached
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        long start = 0, end = fileSize - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            try {
                if (ranges[0].length() > 0) start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && ranges[1].length() > 0) end = Long.parseLong(ranges[1]);
                if (end >= fileSize) end = fileSize - 1;
            } catch (NumberFormatException e) {
                log.warn("Invalid Range header: {}", rangeHeader);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }
        }

        long contentLength = end - start + 1;
        ByteArrayInputStream bis = new ByteArrayInputStream(fullVideoBytes, (int) start, (int) contentLength);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/mp4"));
        headers.setContentLength(contentLength);
        headers.set("Accept-Ranges", "bytes");
        headers.set("ETag", etag);
        headers.set("Cache-Control", "public, max-age=86400");
        headers.set("Content-Disposition", "inline; filename=\"product-video-" + productPrimeId + ".mp4\"");

        if (rangeHeader != null) {
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            return new ResponseEntity<>(new InputStreamResource(bis), headers, HttpStatus.PARTIAL_CONTENT);
        }

        return new ResponseEntity<>(new InputStreamResource(bis), headers, HttpStatus.OK);
    }


    @GetMapping("/get-all-active-products")
    public ResponseEntity<Page<ProductResponseDto>> getAllActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String sortBy,      // e.g. productName, currentSellingPrice
            @RequestParam(defaultValue = "DESC") String sortDir  // ASC or DESC
    ) {
        Page<ProductResponseDto> products = productService.getAllActiveProducts(page, size, sortBy, sortDir);
        return ResponseEntity.ok(products);
    }

    //==================================================//
    //          Bulk Uploading API                      //
    //==================================================//
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponse> bulkUploadProducts(
            @RequestPart("excelFile") MultipartFile excelFile,
            @RequestPart(value = "productImages", required = false) List<MultipartFile> images) {

        log.info("Bulk product upload request received");

        try {
            BulkUploadResponse response = productService.bulkCreateProducts(excelFile, images);
            log.info("Bulk upload completed → uploaded: {}, skipped: {}",
                    response.getUploadedCount(), response.getSkippedCount());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for bulk upload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Unexpected error during bulk upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


// ──────────────────────────────────────────────────────────
//          Category / SubCategory / Addon / GlobalTag APIs
// ──────────────────────────────────────────────────────────

    @GetMapping("/get-by-category")
    public ResponseEntity<Page<ProductCategoryResponse>> getByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("GET /get-by-category | category: {}", category);
        return ResponseEntity.ok(
                productService.getProductsByCategory(category, page, size, sortBy, sortDir));
    }

    @GetMapping("/get-by-sub-category")
    public ResponseEntity<Page<ProductCategoryResponse>> getBySubCategory(
            @RequestParam String subCategory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("GET /get-by-sub-category | subCategory: {}", subCategory);
        return ResponseEntity.ok(
                productService.getProductsBySubCategory(subCategory, page, size, sortBy, sortDir));
    }

    @GetMapping("/get-by-addon")
    public ResponseEntity<Page<ProductCategoryResponse>> getByAddonKey(
            @RequestParam String addonKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("GET /get-by-addon | addonKey: {}", addonKey);
        return ResponseEntity.ok(
                productService.getProductsByAddonKey(addonKey, page, size, sortBy, sortDir));
    }

    @GetMapping("/get-by-glob-tag")
    public ResponseEntity<Page<ProductCategoryResponse>> getByGlobalTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("GET /get-by-glob-tag | tag: {}", tag);
        return ResponseEntity.ok(
                productService.getProductsByGlobalTag(tag, page, size, sortBy, sortDir));
    }



}