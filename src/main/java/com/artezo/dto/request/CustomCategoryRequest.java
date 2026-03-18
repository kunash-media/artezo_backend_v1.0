package com.artezo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public class CustomCategoryRequest {

    @NotBlank(message = "Product category name is required")
    private String productCategory;

    @NotNull(message = "Category path must not be null")
    private List<String> categoryPath;

    @NotBlank(message = "Product category redirect URL is required")
    private String productCategoryRedirect;

    @NotBlank(message = "Category path redirect URL is required")
    private String categoryPathRedirect;

    private Boolean approved = false;

    private Boolean trendingMark = false;

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public List<String> getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(List<String> categoryPath) {
        this.categoryPath = categoryPath;
    }

    public String getProductCategoryRedirect() {
        return productCategoryRedirect;
    }

    public void setProductCategoryRedirect(String productCategoryRedirect) {
        this.productCategoryRedirect = productCategoryRedirect;
    }

    public String getCategoryPathRedirect() {
        return categoryPathRedirect;
    }

    public void setCategoryPathRedirect(String categoryPathRedirect) {
        this.categoryPathRedirect = categoryPathRedirect;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public Boolean getTrendingMark() {
        return trendingMark;
    }

    public void setTrendingMark(Boolean trendingMark) {
        this.trendingMark = trendingMark;
    }
}