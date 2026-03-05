package com.artezo.service;

import com.artezo.dto.request.CreateProductRequestDto;
import com.artezo.dto.response.ProductResponseDto;
import com.artezo.exceptions.ProductCreateResult;

import java.util.List;

public interface ProductService {

    // CREATE
//    ProductResponseDto createProduct(CreateProductRequestDto request);

    ProductCreateResult createProduct(CreateProductRequestDto request);

    // READ
    ProductResponseDto getProductById(Long id);

    ProductResponseDto getProductByStrId(String productStrId);

    // UPDATE
    ProductResponseDto updateProduct(Long id, CreateProductRequestDto request);     // PUT - full replace

    ProductResponseDto patchProduct(Long id, CreateProductRequestDto request);      // PATCH - partial

    // DELETE (soft)
    void deleteProduct(Long id);

    // ────────────────────────────────────────────────
    //      Image data access methods (used by controller)
    // ────────────────────────────────────────────────
    byte[] getProductMainImageData(Long productId);

    List<byte[]> getProductMockupImagesData(Long productId);

    byte[] getVariantMainImageData(Long productId, String variantId);

//    byte[] getInstallationVideoData(Long id, int stepIndex);

    byte[] getInstallationVideoData(Long productId, int stepIndex);

    byte[] getProductVideoData(Long productId);
}