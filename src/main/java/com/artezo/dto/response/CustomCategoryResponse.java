package com.artezo.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;


@Builder
public class CustomCategoryResponse {

    private Long categoryId;
    private String productCategory;
    private List<String> categoryPath;
    private String productCategoryRedirect;
    private String categoryPathRedirect;
    private LocalDateTime createdAt;
    private Boolean approved;
    private Boolean trendingMark;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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