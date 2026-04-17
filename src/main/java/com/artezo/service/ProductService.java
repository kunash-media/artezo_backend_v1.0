package com.artezo.service;

import com.artezo.dto.request.CreateProductRequestDto;
import com.artezo.dto.response.ProductSearchResultDto;
import com.artezo.dto.response.ProductCategoryResponse;
import com.artezo.dto.response.BulkUploadResponse;
import com.artezo.dto.response.ProductResponseDto;
import com.artezo.exceptions.ProductAlreadyDeletedException;
import com.artezo.exceptions.ProductCreateResult;
import com.artezo.exceptions.ProductNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    // CREATE
//    ProductResponseDto createProduct(CreateProductRequestDto request);

    ProductCreateResult createProduct(CreateProductRequestDto request);


    // get product by productPrimeId for admin
    ProductResponseDto getAdminViewProductById(Long productPrimeId);

    // get product by productPrimeId for web view
    ProductResponseDto getProductById(Long productPrimeId, Long userId);

    ProductResponseDto getProductByStrId(String productStrId);

    // UPDATE
    ProductResponseDto updateProduct(Long productPrimeId, CreateProductRequestDto request);     // PUT - full replace

    ProductResponseDto patchProduct(Long productPrimeId, CreateProductRequestDto request);      // PATCH - partial

    // DELETE (soft)
    void deleteProduct(Long productPrimeId) throws ProductAlreadyDeletedException, ProductNotFoundException;

    // ────────────────────────────────────────────────
    //      Image data access methods (used by controller)
    // ────────────────────────────────────────────────
    byte[] getProductMainImageData(Long productId);

    List<byte[]> getProductMockupImagesData(Long productId);

    byte[] getVariantMainImageData(Long productId, String variantId);

//    byte[] getInstallationVideoData(Long id, int stepIndex);

    byte[] getInstallationVideoData(Long productId, int stepIndex);

    byte[] getProductVideoData(Long productId);


    Page<ProductResponseDto> getAllActiveProducts(int page, int size, String sortBy, String sortDir);

    // Hero Banner Image
    byte[] getHeroBannerImage(Long productPrimeId, String bannerId);

    // Installation Step Image

    byte[]getInstallationStepImage(Long productId, Integer step);


    public BulkUploadResponse bulkCreateProducts(MultipartFile excelFile, List<MultipartFile> images);

    //fetch by category and addon, global tag

    // ProductService.java

    Page<ProductCategoryResponse> getProductsByCategory(String category, int page, int size, String sortBy, String sortDir);

    Page<ProductCategoryResponse> getProductsBySubCategory(String subCategory, int page, int size, String sortBy, String sortDir);

    Page<ProductCategoryResponse> getProductsByAddonKey(String addonKey, int page, int size, String sortBy, String sortDir);

    Page<ProductCategoryResponse> getProductsByGlobalTag(String tag, int page, int size, String sortBy, String sortDir);


    List<ProductSearchResultDto> searchProducts(String keyword, int limit);
}