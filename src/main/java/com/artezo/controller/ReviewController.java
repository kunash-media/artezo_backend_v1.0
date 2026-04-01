package com.artezo.controller;

import com.artezo.dto.request.ReviewRequestDto;
import com.artezo.dto.response.ReviewResponseDto;
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

    // Create new review with images/videos
    @PostMapping(value = "/create-review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> createReview(
            @RequestParam("productId") Long productId,
            @RequestParam("userId") Long userId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "videos", required = false) List<MultipartFile> videos) {

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setProductId(productId);
        requestDto.setUserId(userId);
        requestDto.setRating(rating);
        requestDto.setComment(comment);
        requestDto.setImages(images);
        requestDto.setVideos(videos);

        ReviewResponseDto createdReview = reviewService.createReview(requestDto);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    // Create review with JSON + Base64 (for local testing)
    @PostMapping("/base64")
    public ResponseEntity<ReviewResponseDto> createReviewWithBase64(@Valid @RequestBody ReviewRequestDto requestDto) {
        ReviewResponseDto createdReview = reviewService.createReview(requestDto);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    // Get review by ID
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReviewById(@PathVariable Long reviewId) {
        ReviewResponseDto review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    // Get all reviews for a product
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

    // Update review
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long reviewId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "videos", required = false) List<MultipartFile> videos) {

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setRating(rating);
        requestDto.setComment(comment);
        requestDto.setImages(images);
        requestDto.setVideos(videos);

        ReviewResponseDto updatedReview = reviewService.updateReview(reviewId, requestDto);
        return ResponseEntity.ok(updatedReview);
    }

    // Delete review
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    // Get product review summary
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<Map<String, Object>> getProductReviewSummary(@PathVariable Long productId) {
        Map<String, Object> summary = reviewService.getProductReviewSummary(productId);
        return ResponseEntity.ok(summary);
    }

    // Get media file (for local mode)
    @GetMapping("/{reviewId}/media/{type}")
    public ResponseEntity<byte[]> getReviewMedia(
            @PathVariable Long reviewId,
            @PathVariable String type) {

        byte[] media = reviewService.getReviewMedia(reviewId, type);

        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = "image".equalsIgnoreCase(type) ? "image/jpeg" : "video/mp4";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(media);
    }

    // Add images to existing review
    @PostMapping(value = "/{reviewId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> addImagesToReview(
            @PathVariable Long reviewId,
            @RequestParam("images") List<MultipartFile> images) {

        ReviewResponseDto updatedReview = reviewService.addImagesToReview(reviewId, images);
        return ResponseEntity.ok(updatedReview);
    }

    // Add videos to existing review
    @PostMapping(value = "/{reviewId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> addVideosToReview(
            @PathVariable Long reviewId,
            @RequestParam("videos") List<MultipartFile> videos) {

        ReviewResponseDto updatedReview = reviewService.addVideosToReview(reviewId, videos);
        return ResponseEntity.ok(updatedReview);
    }

    // Remove media from review
    @DeleteMapping("/{reviewId}/media")
    public ResponseEntity<Void> removeMediaFromReview(
            @PathVariable Long reviewId,
            @RequestParam("mediaUrl") String mediaUrl) {

        reviewService.removeMediaFromReview(reviewId, mediaUrl);
        return ResponseEntity.noContent().build();
    }

    // Get recent reviews (limited to 10)
    @GetMapping("/product/{productId}/recent")
    public ResponseEntity<List<ReviewResponseDto>> getRecentReviews(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getRecentReviews(productId);
        return ResponseEntity.ok(reviews);
    }

    // Get reviews with images only
    @GetMapping("/product/{productId}/with-images")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsWithImages(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsWithImages(productId);
        return ResponseEntity.ok(reviews);
    }

    // Get reviews with videos only
    @GetMapping("/product/{productId}/with-videos")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsWithVideos(@PathVariable Long productId) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsWithVideos(productId);
        return ResponseEntity.ok(reviews);
    }
}