package com.artezo.dto.request;

import java.util.List;

public class ProductReviewRequestDto {

    private Integer reviewId;
    private Integer userId;
    private String description;
    private List<byte[]> reviewImages;
    private boolean approved;

    public Integer getReviewId() {
        return reviewId;
    }

    public void setReviewId(Integer reviewId) {
        this.reviewId = reviewId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<byte[]> getReviewImages() {
        return reviewImages;
    }

    public void setReviewImages(List<byte[]> reviewImages) {
        this.reviewImages = reviewImages;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}