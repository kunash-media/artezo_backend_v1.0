package com.artezo.dto.response;

import com.artezo.entity.ReviewEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponseDto {

    private Long reviewId;
    private Long productId;
    private Long userId;
    private Integer rating;
    private String comment;

    // URLs
    private String imageUrl;
    private String videoUrl;

    // Content types
    private String imageContentType;
    private String videoContentType;

    // Original and compressed sizes
    private Long imageOriginalSize;
    private Long imageCompressedSize;
    private Long videoOriginalSize;
    private Long videoCompressedSize;

    // Formatted strings for display
    private String imageOriginalSizeFormatted;
    private String imageCompressedSizeFormatted;
    private String videoOriginalSizeFormatted;
    private String videoCompressedSizeFormatted;

    // Names
    private String imageName;
    private String videoName;

    // ==================== ADMIN PANEL FIELDS ====================
    private String status;
    private Boolean approved;  // 🔴 NEW: Add approved field
    private Boolean flagged;
    private List<ReplyDto> replies;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewResponseDto() {}

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

    public static ReviewResponseDto fromEntity(ReviewEntity entity) {
        ReviewResponseDto dto = new ReviewResponseDto();

        dto.setReviewId(entity.getReviewId());
        dto.setProductId(entity.getProductId());
        dto.setUserId(entity.getUserId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Set names
        dto.setImageName(entity.getImageName());
        dto.setVideoName(entity.getVideoName());

        // Set content types
        dto.setImageContentType(entity.getImageContentType());
        dto.setVideoContentType(entity.getVideoContentType());

        // Set original and compressed sizes
        if (entity.getImageOriginalSize() != null) {
            dto.setImageOriginalSize(entity.getImageOriginalSize());
            dto.setImageOriginalSizeFormatted(dto.formatSize(entity.getImageOriginalSize()));
        }

        if (entity.getImageCompressedSize() != null) {
            dto.setImageCompressedSize(entity.getImageCompressedSize());
            dto.setImageCompressedSizeFormatted(dto.formatSize(entity.getImageCompressedSize()));
        }

        if (entity.getVideoOriginalSize() != null) {
            dto.setVideoOriginalSize(entity.getVideoOriginalSize());
            dto.setVideoOriginalSizeFormatted(dto.formatSize(entity.getVideoOriginalSize()));
        }

        if (entity.getVideoCompressedSize() != null) {
            dto.setVideoCompressedSize(entity.getVideoCompressedSize());
            dto.setVideoCompressedSizeFormatted(dto.formatSize(entity.getVideoCompressedSize()));
        }

        // Set URLs
        if (entity.getImageData() != null && entity.getImageData().length > 0) {
            dto.setImageUrl("/api/reviews/" + entity.getReviewId() + "/media/image");
        }

        if (entity.getVideoData() != null && entity.getVideoData().length > 0) {
            dto.setVideoUrl("/api/reviews/" + entity.getReviewId() + "/media/video");
        }

        // ==================== SET ADMIN FIELDS ====================
        dto.setStatus(entity.getStatus());
        dto.setApproved(entity.getApproved());  // 🔴 NEW: Set approved field
        dto.setFlagged(entity.getFlagged());

        // Parse replies from JSON string if present
        if (entity.getReplies() != null && !entity.getReplies().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<ReplyDto> replies = mapper.readValue(entity.getReplies(),
                        new TypeReference<List<ReplyDto>>() {});
                dto.setReplies(replies);
            } catch (Exception e) {
                dto.setReplies(new ArrayList<>());
            }
        } else {
            dto.setReplies(new ArrayList<>());
        }

        return dto;
    }

    // ==================== INNER CLASS FOR REPLIES ====================
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReplyDto {
        private Long id;
        private String content;
        private String adminName;
        private LocalDateTime date;

        public ReplyDto() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getAdminName() {
            return adminName;
        }

        public void setAdminName(String adminName) {
            this.adminName = adminName;
        }

        public LocalDateTime getDate() {
            return date;
        }

        public void setDate(LocalDateTime date) {
            this.date = date;
        }
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
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

    public String getImageOriginalSizeFormatted() {
        return imageOriginalSizeFormatted;
    }

    public void setImageOriginalSizeFormatted(String imageOriginalSizeFormatted) {
        this.imageOriginalSizeFormatted = imageOriginalSizeFormatted;
    }

    public String getImageCompressedSizeFormatted() {
        return imageCompressedSizeFormatted;
    }

    public void setImageCompressedSizeFormatted(String imageCompressedSizeFormatted) {
        this.imageCompressedSizeFormatted = imageCompressedSizeFormatted;
    }

    public String getVideoOriginalSizeFormatted() {
        return videoOriginalSizeFormatted;
    }

    public void setVideoOriginalSizeFormatted(String videoOriginalSizeFormatted) {
        this.videoOriginalSizeFormatted = videoOriginalSizeFormatted;
    }

    public String getVideoCompressedSizeFormatted() {
        return videoCompressedSizeFormatted;
    }

    public void setVideoCompressedSizeFormatted(String videoCompressedSizeFormatted) {
        this.videoCompressedSizeFormatted = videoCompressedSizeFormatted;
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
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getApproved() {  // 🔴 NEW: Getter for approved
        return approved;
    }

    public void setApproved(Boolean approved) {  // 🔴 NEW: Setter for approved
        this.approved = approved;
    }

    public Boolean getFlagged() {
        return flagged;
    }

    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
    }

    public List<ReplyDto> getReplies() {
        return replies;
    }

    public void setReplies(List<ReplyDto> replies) {
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
}