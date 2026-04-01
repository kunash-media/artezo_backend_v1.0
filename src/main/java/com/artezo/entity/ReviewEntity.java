package com.artezo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating;



    @Column(columnDefinition = "TEXT")
    private String comment;

    // Store as comma-separated URLs (use TEXT for longer content)
    @Column(columnDefinition = "TEXT")
    private String imageUrls;  // "https://example.com/image1.jpg,https://example.com/image2.jpg"

    @Column(columnDefinition = "TEXT")
    private String videoUrls;  // "https://example.com/video1.mp4,https://example.com/video2.mp4"

    // Remove these binary fields - we don't store actual data anymore
    // @Lob
    // @Column(columnDefinition = "LONGBLOB")
    // private byte[] imageData;

    // @Lob
    // @Column(columnDefinition = "LONGBLOB")
    // private byte[] videoData;

    // Keep metadata
    private String imageContentType;
    private String videoContentType;
    private Long imageSize;
    private Long videoSize;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods to handle multiple images/videos
    public String[] getImageUrlsArray() {
        return imageUrls != null ? imageUrls.split(",") : new String[0];
    }

    public void setImageUrlsArray(String[] urls) {
        this.imageUrls = String.join(",", urls);
    }

    public String[] getVideoUrlsArray() {
        return videoUrls != null ? videoUrls.split(",") : new String[0];
    }

    public void setVideoUrlsArray(String[] urls) {
        this.videoUrls = String.join(",", urls);
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

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getVideoUrls() {
        return videoUrls;
    }

    public void setVideoUrls(String videoUrls) {
        this.videoUrls = videoUrls;
    }

    // Remove getters/setters for imageData/videoData
    // public byte[] getImageData() { return imageData; }
    // public void setImageData(byte[] imageData) { this.imageData = imageData; }
    // public byte[] getVideoData() { return videoData; }
    // public void setVideoData(byte[] videoData) { this.videoData = videoData; }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public String getVideoContentType() {
        return videoContentType;
    }

    public void setVideoContentType(String videoContentType) {
        this.videoContentType = videoContentType;
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

    @Override
    public String toString() {
        return "ReviewEntity{" +
                "reviewId=" + reviewId +
                ", productId=" + productId +
                ", userId=" + userId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", imageUrls='" + imageUrls + '\'' +
                ", videoUrls='" + videoUrls + '\'' +
                ", imageSize=" + imageSize +
                ", videoSize=" + videoSize +
                ", createdAt=" + createdAt +
                '}';
    }
}