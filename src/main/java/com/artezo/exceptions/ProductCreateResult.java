package com.artezo.exceptions;

import com.artezo.dto.response.ProductResponseDto;
import org.springframework.http.HttpStatus;

// Add this small immutable class (can be inner class or separate file)
public class ProductCreateResult {
    private final ProductResponseDto dto;
    private final String errorMessage;
    private final HttpStatus status;

    private ProductCreateResult(ProductResponseDto dto, String error, HttpStatus status) {
        this.dto = dto;
        this.errorMessage = error;
        this.status = status;
    }

    public static ProductCreateResult success(ProductResponseDto dto) {
        return new ProductCreateResult(dto, null, HttpStatus.CREATED);
    }

    public static ProductCreateResult error(String message, HttpStatus status) {
        return new ProductCreateResult(null, message, status);
    }

    public boolean isSuccess() { return dto != null; }
    public ProductResponseDto getDto() { return dto; }
    public String getErrorMessage() { return errorMessage; }
    public HttpStatus getStatus() { return status; }
}