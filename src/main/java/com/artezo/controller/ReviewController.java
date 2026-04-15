package com.artezo.controller;

import com.artezo.dto.request.ReviewRequestDto;
import com.artezo.dto.response.ReviewResponseDto;
import com.artezo.entity.ReviewEntity;
import com.artezo.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // ==================== CREATE REVIEWS ====================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> createReview(
            @RequestParam("productId") Long productId,
            @RequestParam("userId") Long userId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "orderId", required = false) Long orderId,        // ⬅️ ADD THIS
            @RequestParam(value = "orderItemId", required = false) Long orderItemId, // ⬅️ ADD THIS
            @RequestPart(value = "images", required = false) MultipartFile images,
            @RequestPart(value = "videos", required = false) MultipartFile videos) {

        System.out.println("🔴 CONTROLLER - Received request:");
        System.out.println("   productId: " + productId);
        System.out.println("   userId: " + userId);
        System.out.println("   orderId: " + orderId);           // ⬅️ ADD THIS
        System.out.println("   orderItemId: " + orderItemId);   // ⬅️ ADD THIS
        System.out.println("   rating: " + rating);
        System.out.println("   comment: " + comment);
        System.out.println("   images: " + (images != null ? images.getOriginalFilename() : "null"));
        System.out.println("   videos: " + (videos != null ? videos.getOriginalFilename() : "null"));

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setProductId(productId);
        requestDto.setUserId(userId);
        requestDto.setRating(rating);
        requestDto.setComment(comment);
        requestDto.setOrderId(orderId);           // ⬅️ ADD THIS
        requestDto.setOrderItemId(orderItemId);   // ⬅️ ADD THIS

        if (images != null && !images.isEmpty()) {
            requestDto.setImages(List.of(images));
        }

        if (videos != null && !videos.isEmpty()) {
            requestDto.setVideos(List.of(videos));
        }

        ReviewResponseDto createdReview = reviewService.createReview(requestDto);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    // Create review with JSON + Base64 (for local testing without file upload)
    @PostMapping("/base64")
    public ResponseEntity<ReviewResponseDto> createReviewWithBase64(@Valid @RequestBody ReviewRequestDto requestDto) {
        System.out.println("🔴 CONTROLLER - Base64 request:");
        System.out.println("   productId: " + requestDto.getProductId());
        System.out.println("   userId: " + requestDto.getUserId());
        System.out.println("   rating: " + requestDto.getRating());
        System.out.println("   comment: " + requestDto.getComment());
        System.out.println("   imageBase64: " + (requestDto.getImageBase64() != null ? "present" : "null"));
        System.out.println("   videoBase64: " + (requestDto.getVideoBase64() != null ? "present" : "null"));

        ReviewResponseDto createdReview = reviewService.createReview(requestDto);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    // ==================== GET REVIEWS ====================

    // Get review by ID
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReviewById(@PathVariable Long reviewId) {
        ReviewResponseDto review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    // Get all reviews for a product (PUBLIC - only approved reviews)
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByProduct(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsByProduct(productId);
        return ResponseEntity.ok(reviews);
    }

    // Get all reviews by a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByUser(@PathVariable Long userId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(reviews);
    }

    // Get recent reviews (limited to 10) - PUBLIC only approved
    @GetMapping("/product/{productId}/recent")
    public ResponseEntity<List<ReviewResponseDto>> getRecentReviews(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getRecentReviews(productId);
        return ResponseEntity.ok(reviews);
    }

    // Get reviews with images only - PUBLIC only approved
    @GetMapping("/product/{productId}/with-images")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsWithImages(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsWithImages(productId);
        return ResponseEntity.ok(reviews);
    }

    // Get reviews with videos only - PUBLIC only approved
    @GetMapping("/product/{productId}/with-videos")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsWithVideos(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsWithVideos(productId);
        return ResponseEntity.ok(reviews);
    }

    // ==================== UPDATE REVIEWS ====================

    // Update review with multipart files
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long reviewId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestPart(value = "images", required = false) MultipartFile images,
            @RequestPart(value = "videos", required = false) MultipartFile videos) {

        System.out.println("🔴 CONTROLLER - Update request for review: " + reviewId);

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setRating(rating);
        requestDto.setComment(comment);

        if (images != null && !images.isEmpty()) {
            requestDto.setImages(List.of(images));
        }

        if (videos != null && !videos.isEmpty()) {
            requestDto.setVideos(List.of(videos));
        }

        ReviewResponseDto updatedReview = reviewService.updateReview(reviewId, requestDto);
        return ResponseEntity.ok(updatedReview);
    }

    // Update review with Base64
    @PutMapping("/base64/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReviewWithBase64(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequestDto requestDto) {
        ReviewResponseDto updatedReview = reviewService.updateReview(reviewId, requestDto);
        return ResponseEntity.ok(updatedReview);
    }

    // ==================== MEDIA MANAGEMENT ====================

    // Get media as raw bytes (for viewing images/videos)
    @GetMapping("/{reviewId}/media/{type}")
    public ResponseEntity<byte[]> getReviewMedia(
            @PathVariable Long reviewId,
            @PathVariable String type) {

        byte[] media = reviewService.getReviewMedia(reviewId, type);

        if (media == null || media.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
        String contentType;
        if ("image".equalsIgnoreCase(type)) {
            contentType = "image/jpeg";
        } else if ("video".equalsIgnoreCase(type)) {
            contentType = "video/mp4";
        } else {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(media);
    }

    // Add image to existing review
    @PostMapping(value = "/{reviewId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> addImagesToReview(
            @PathVariable Long reviewId,
            @RequestPart("images") MultipartFile images) {

        System.out.println("🔴 CONTROLLER - Adding image to review: " + reviewId);

        ReviewResponseDto updatedReview = reviewService.addImagesToReview(reviewId, List.of(images));
        return ResponseEntity.ok(updatedReview);
    }

    // Add video to existing review
    @PostMapping(value = "/{reviewId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> addVideosToReview(
            @PathVariable Long reviewId,
            @RequestPart("videos") MultipartFile videos) {

        System.out.println("🔴 CONTROLLER - Adding video to review: " + reviewId);

        ReviewResponseDto updatedReview = reviewService.addVideosToReview(reviewId, List.of(videos));
        return ResponseEntity.ok(updatedReview);
    }

    // Remove media from review
    @DeleteMapping("/{reviewId}/media/{mediaType}")
    public ResponseEntity<Void> removeMediaFromReview(
            @PathVariable Long reviewId,
            @PathVariable String mediaType) {

        System.out.println("🔴 CONTROLLER - Removing " + mediaType + " from review: " + reviewId);

        reviewService.removeMediaFromReview(reviewId, mediaType);
        return ResponseEntity.noContent().build();
    }

    // ==================== REVIEW SUMMARY ====================

    // Get product review summary (average rating, total reviews, distribution)
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<Map<String, Object>> getProductReviewSummary(@PathVariable Long productId) {
        Map<String, Object> summary = reviewService.getProductReviewSummary(productId);
        return ResponseEntity.ok(summary);
    }

    // ==================== DELETE REVIEW ====================

    // Delete review
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        System.out.println("🔴 CONTROLLER - Deleting review: " + reviewId);
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    // ==================== ADMIN PANEL ENDPOINTS ====================

    // Get all reviews (for admin panel)
    @GetMapping("/all")
    public ResponseEntity<List<ReviewResponseDto>> getAllReviews() {
        List<ReviewResponseDto> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(reviews);
    }

    // Update review status (approve/reject) - ADMIN ONLY
    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<ReviewResponseDto> updateReviewStatus(
            @PathVariable Long reviewId,
            @RequestParam String status) {
        ReviewResponseDto review = reviewService.updateReviewStatus(reviewId, status);
        return ResponseEntity.ok(review);
    }

    // Reply to review - ADMIN ONLY
    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponseDto> addReplyToReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> replyData) {
        String replyComment = replyData.get("comment");
        if (replyComment == null) {
            replyComment = replyData.get("content");
        }
        ReviewResponseDto review = reviewService.addReplyToReview(reviewId, replyComment);
        return ResponseEntity.ok(review);
    }

    // Clear flag on review - ADMIN ONLY
    @PutMapping("/{reviewId}/clear-flag")
    public ResponseEntity<ReviewResponseDto> clearFlag(@PathVariable Long reviewId) {
        ReviewResponseDto review = reviewService.clearFlag(reviewId);
        return ResponseEntity.ok(review);
    }

    // Bulk approve reviews - ADMIN ONLY
    @PostMapping("/bulk-approve")
    public ResponseEntity<List<ReviewResponseDto>> bulkApproveReviews(@RequestBody List<Long> reviewIds) {
        List<ReviewResponseDto> reviews = reviewService.bulkApproveReviews(reviewIds);
        return ResponseEntity.ok(reviews);
    }
}