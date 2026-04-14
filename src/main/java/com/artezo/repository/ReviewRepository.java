package com.artezo.repository;

import com.artezo.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByProductId(Long productId);
    List<ReviewEntity> findByUserId(Long userId);
    List<ReviewEntity> findByProductIdAndRating(Long productId, Integer rating);
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.productId = :productId")
    Double getAverageRatingForProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId")
    Long getReviewCountForProduct(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId ORDER BY r.createdAt DESC")
    List<ReviewEntity> findTopRecentReviews(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.imageData IS NOT NULL")
    List<ReviewEntity> findReviewsWithImages(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.videoData IS NOT NULL")
    List<ReviewEntity> findReviewsWithVideos(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId AND r.imageData IS NOT NULL")
    long countByProductIdAndHasImage(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId AND r.videoData IS NOT NULL")
    long countByProductIdAndHasVideo(@Param("productId") Long productId);

    void deleteByProductId(Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);

    Long countByProductId(Long productId);
    List<ReviewEntity> findByProductIdAndRatingBetween(Long productId, Integer minRating, Integer maxRating);

    // ==================== NEW METHODS FOR APPROVED REVIEWS (PUBLIC API) ====================

    // Get average rating for approved reviews only
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.productId = :productId AND r.approved = true")
    Double getAverageRatingForApprovedReviews(@Param("productId") Long productId);

    // Get count of approved reviews for a product
    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId AND r.approved = true")
    Long getApprovedReviewCountForProduct(@Param("productId") Long productId);

    // Get recent approved reviews for a product (for public display)
    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.approved = true ORDER BY r.createdAt DESC")
    List<ReviewEntity> findTopRecentApprovedReviews(@Param("productId") Long productId);

    // Get approved reviews with images only (for public display)
    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.imageData IS NOT NULL AND r.approved = true")
    List<ReviewEntity> findApprovedReviewsWithImages(@Param("productId") Long productId);

    // Get approved reviews with videos only (for public display)
    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.videoData IS NOT NULL AND r.approved = true")
    List<ReviewEntity> findApprovedReviewsWithVideos(@Param("productId") Long productId);

    // Get all approved reviews (for public API)
    @Query("SELECT r FROM ReviewEntity r WHERE r.approved = true ORDER BY r.createdAt DESC")
    List<ReviewEntity> findAllApprovedReviews();

    // Get pending reviews for admin
    @Query("SELECT r FROM ReviewEntity r WHERE r.status = 'pending' ORDER BY r.createdAt DESC")
    List<ReviewEntity> findPendingReviews();

    // Get flagged reviews for admin
    @Query("SELECT r FROM ReviewEntity r WHERE r.flagged = true ORDER BY r.createdAt DESC")
    List<ReviewEntity> findFlaggedReviews();

    // Get reviews by status
    List<ReviewEntity> findByStatus(String status);

    // Get reviews by approved flag
    List<ReviewEntity> findByApproved(Boolean approved);

    // Get reviews by flagged flag
    List<ReviewEntity> findByFlagged(Boolean flagged);

    // Count pending reviews
    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.status = 'pending'")
    Long countPendingReviews();

    // Count flagged reviews
    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.flagged = true")
    Long countFlaggedReviews();

    // Get reviews with pagination support (optional, for admin panel)
    @Query("SELECT r FROM ReviewEntity r ORDER BY r.createdAt DESC")
    List<ReviewEntity> findAllOrderByCreatedAtDesc();
}