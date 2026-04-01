package com.artezo.dto.response;

import com.artezo.entity.ReviewEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ReviewResponseDto {

    private Long reviewId;
    private Long productId;
    private Long userId;
    private Integer rating;
    private String comment;

    // Media fields
    private List<String> imageUrls;
    private List<String> videoUrls;

    // Original sizes in bytes
    private Long imageSize;
    private Long videoSize;

    // 🔴 FORMATTED SIZES ADD KAR
    private String imageSizeFormatted;
    private String videoSizeFormatted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ReviewResponseDto() {
        this.imageUrls = new ArrayList<>();
        this.videoUrls = new ArrayList<>();
    }

    // 🔴 HELPER METHOD TO FORMAT SIZE
    private String formatSize(long size) {
        if (size <= 0) return "0 KB";

        double mb = size / (1024.0 * 1024.0);
        if (mb >= 1) {
            return String.format("%.2f MB", mb);
        } else {
            double kb = size / 1024.0;
            return String.format("%.2f KB", kb);
        }
    }

    // Static method to convert from Entity
    public static ReviewResponseDto fromEntity(ReviewEntity entity) {
        ReviewResponseDto dto = new ReviewResponseDto();

        dto.setReviewId(entity.getReviewId());
        dto.setProductId(entity.getProductId());
        dto.setUserId(entity.getUserId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Set original sizes
        dto.setImageSize(entity.getImageSize());
        dto.setVideoSize(entity.getVideoSize());

        // 🔴 SET FORMATTED SIZES
        if (entity.getImageSize() != null) {
            dto.setImageSizeFormatted(dto.formatSize(entity.getImageSize()));
        } else {
            dto.setImageSizeFormatted("0 KB");
        }

        if (entity.getVideoSize() != null) {
            dto.setVideoSizeFormatted(dto.formatSize(entity.getVideoSize()));
        } else {
            dto.setVideoSizeFormatted("0 KB");
        }

        // Handle image URLs
        if (entity.getImageUrls() != null && !entity.getImageUrls().isEmpty()) {
            dto.setImageUrls(Arrays.asList(entity.getImageUrlsArray()));
        }

        // Handle video URLs
        if (entity.getVideoUrls() != null && !entity.getVideoUrls().isEmpty()) {
            dto.setVideoUrls(Arrays.asList(entity.getVideoUrlsArray()));
        }

        return dto;
    }

    // Getters and Setters
    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getVideoUrls() {
        return videoUrls;
    }

    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
    }

    public Long getImageSize() {
        return imageSize;
    }

    public void setImageSize(Long imageSize) {
        this.imageSize = imageSize;
    }

    public Long getVideoSize() {
        return videoSize;
    }

    public void setVideoSize(Long videoSize) {
        this.videoSize = videoSize;
    }

    // 🔴 GETTERS/SETTERS FOR FORMATTED SIZES
    public String getImageSizeFormatted() {
        return imageSizeFormatted;
    }

    public void setImageSizeFormatted(String imageSizeFormatted) {
        this.imageSizeFormatted = imageSizeFormatted;
    }

    public String getVideoSizeFormatted() {
        return videoSizeFormatted;
    }

    public void setVideoSizeFormatted(String videoSizeFormatted) {
        this.videoSizeFormatted = videoSizeFormatted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}