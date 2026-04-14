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

    // Store actual image data in database as BLOB
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] videoData;

    // Store metadata
    private String imageContentType;
    private String videoContentType;

    // 🔴 FIXED: Only declare once
    @Column(nullable = false)
    private Boolean approved = false;  // DEFAULT FALSE - NOT APPROVED

    // 🔴 FIXED: Only declare once (removed duplicate)
    @Column(nullable = false)
    private String status = "pending";  // pending, approved, rejected

    // Store both original and compressed sizes
    private Long imageOriginalSize;      // Original size before compression
    private Long imageCompressedSize;    // Size after compression
    private Long videoOriginalSize;      // Original size before compression
    private Long videoCompressedSize;    // Size after compression

    // Keep for backward compatibility (optional)
    private Long imageSize;
    private Long videoSize;

    // Store image/video names
    private String imageName;
    private String videoName;

    // ==================== ADMIN FIELDS ====================
    private Boolean flagged = false;

    @Column(columnDefinition = "TEXT")
    private String replies; // Store replies as JSON string

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "pending";
        }
        if (approved == null) {
            approved = false;
        }
        if (flagged == null) {
            flagged = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public byte[] getVideoData() {
        return videoData;
    }

    public void setVideoData(byte[] videoData) {
        this.videoData = videoData;
    }

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

    public Long getImageOriginalSize() {
        return imageOriginalSize;
    }

    public void setImageOriginalSize(Long imageOriginalSize) {
        this.imageOriginalSize = imageOriginalSize;
    }

    public Long getImageCompressedSize() {
        return imageCompressedSize;
    }

    public void setImageCompressedSize(Long imageCompressedSize) {
        this.imageCompressedSize = imageCompressedSize;
    }

    public Long getVideoOriginalSize() {
        return videoOriginalSize;
    }

    public void setVideoOriginalSize(Long videoOriginalSize) {
        this.videoOriginalSize = videoOriginalSize;
    }

    public Long getVideoCompressedSize() {
        return videoCompressedSize;
    }

    public void setVideoCompressedSize(Long videoCompressedSize) {
        this.videoCompressedSize = videoCompressedSize;
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

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    // ==================== GETTERS AND SETTERS FOR ADMIN FIELDS ====================

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getFlagged() {
        return flagged;
    }

    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
    }

    public String getReplies() {
        return replies;
    }

    public void setReplies(String replies) {
        this.replies = replies;
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
                ", imageOriginalSize=" + imageOriginalSize +
                ", imageCompressedSize=" + imageCompressedSize +
                ", videoOriginalSize=" + videoOriginalSize +
                ", videoCompressedSize=" + videoCompressedSize +
                ", approved=" + approved +
                ", status='" + status + '\'' +
                ", flagged=" + flagged +
                ", createdAt=" + createdAt +
                '}';
    }
}
