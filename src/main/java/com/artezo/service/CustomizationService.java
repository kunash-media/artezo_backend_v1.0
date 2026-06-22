package com.artezo.service;

import com.artezo.dto.request.AddCustomizedToCartRequest;
import com.artezo.dto.response.CartResponse;
import com.artezo.dto.response.CustomizationUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CustomizationService {
    CustomizationUploadResponse uploadCustomizationImage(MultipartFile file,
                                                         Long userId,
                                                         String sessionId);

    CartResponse addCustomizedToCart(AddCustomizedToCartRequest request);

    byte[] getCustomizationImage(String assetUuid); // for serving image to admin/user
}