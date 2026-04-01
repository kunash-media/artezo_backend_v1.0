package com.artezo.service;

import com.artezo.dto.request.ReviewRequestDto;
import com.artezo.dto.response.ReviewResponseDto;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ReviewService {

    // Create review with media
    ReviewResponseDto createReview(ReviewRequestDto requestDto);

    // Get review by ID
    ReviewResponseDto getReviewById(Long reviewId);

    // Get all reviews for a product
    List<ReviewResponseDto> getReviewsByProduct(Long productId);

    // Get all reviews by a user
    List<ReviewResponseDto> getReviewsByUser(Long userId);

    // Update review
    ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto requestDto);

    // Delete review
    void deleteReview(Long reviewId);

    // Get product review summary
    Map<String, Object> getProductReviewSummary(Long productId);

    // Get media file (for local mode)
    byte[] getReviewMedia(Long reviewId, String mediaType);

    // Upload additional images to existing review
    ReviewResponseDto addImagesToReview(Long reviewId, List<MultipartFile> images);

    // Upload additional videos to existing review
    ReviewResponseDto addVideosToReview(Long reviewId, List<MultipartFile> videos);

    // Remove media from review
    void removeMediaFromReview(Long reviewId, String mediaUrl);

    // Get recent reviews for a product (limited)
    List<ReviewResponseDto> getRecentReviews(Long productId);

    // Get reviews with images only
    List<ReviewResponseDto> getReviewsWithImages(Long productId);

    // Get reviews with videos only
    List<ReviewResponseDto> getReviewsWithVideos(Long productId);
}
