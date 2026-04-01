package com.artezo.repository;

import com.artezo.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    // Find all reviews for a product
    List<ReviewEntity> findByProductId(Long productId);

    // Find all reviews by a user
    List<ReviewEntity> findByUserId(Long userId);

    // Find reviews by product with rating
    List<ReviewEntity> findByProductIdAndRating(Long productId, Integer rating);

    // Check if user already reviewed a product
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    // Get average rating for a product
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.productId = :productId")
    Double getAverageRatingForProduct(@Param("productId") Long productId);

    // Get review count for a product
    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId")
    Long getReviewCountForProduct(@Param("productId") Long productId);

    // Find top recent reviews for a product (limited to 10)
    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId ORDER BY r.createdAt DESC")
    List<ReviewEntity> findTopRecentReviews(@Param("productId") Long productId);

    // Find reviews with images
    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.imageUrls IS NOT NULL")
    List<ReviewEntity> findReviewsWithImages(@Param("productId") Long productId);

    // Find reviews with videos
    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.videoUrls IS NOT NULL")
    List<ReviewEntity> findReviewsWithVideos(@Param("productId") Long productId);

    // Delete all reviews for a product
    void deleteByProductId(Long productId);

    // Get rating distribution for a product
    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);

    // Find reviews with pagination (for future use)
    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId ORDER BY r.createdAt DESC")
    List<ReviewEntity> findRecentReviewsWithPagination(@Param("productId") Long productId);

    // Count total reviews for a product
    Long countByProductId(Long productId);

    // Find reviews by rating range
    List<ReviewEntity> findByProductIdAndRatingBetween(Long productId, Integer minRating, Integer maxRating);
}