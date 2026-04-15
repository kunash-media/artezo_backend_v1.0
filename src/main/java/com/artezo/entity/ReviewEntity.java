package com.artezo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    // ==================== RELATIONSHIPS ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItemEntity orderItem;  // To track which specific item was reviewed

    // ==================== FIELDS ====================

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

    @Column(nullable = false)
    private Boolean approved = false;  // DEFAULT FALSE - NOT APPROVED

    @Column(nullable = false)
    private String status = "pending";  // pending, approved, rejected

    // Store both original and compressed sizes
    private Long imageOriginalSize;
    private Long imageCompressedSize;
    private Long videoOriginalSize;
    private Long videoCompressedSize;

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

    @Column(columnDefinition = "boolean default false")
    private boolean isVerifiedPurchase = false;  // TRUE if product was actually purchased

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

    // ==================== GETTERS AND SETTERS ====================

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public OrderItemEntity getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItemEntity orderItem) {
        this.orderItem = orderItem;
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

    public boolean isVerifiedPurchase() {
        return isVerifiedPurchase;
    }

    public void setVerifiedPurchase(boolean verifiedPurchase) {
        isVerifiedPurchase = verifiedPurchase;
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

    // Helper method to get product ID (for backward compatibility)
    public Long getProductId() {
        return product != null ? product.getProductPrimeId() : null;
    }

    // Helper method to get user ID (for backward compatibility)
    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    // Helper method to get order ID (for backward compatibility)
    public Long getOrderId() {
        return order != null ? order.getOrderId() : null;
    }

    @Override
    public String toString() {
        return "ReviewEntity{" +
                "reviewId=" + reviewId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", approved=" + approved +
                ", status='" + status + '\'' +
                ", flagged=" + flagged +
                ", isVerifiedPurchase=" + isVerifiedPurchase +
                ", createdAt=" + createdAt +
                '}';
    }
}
