package com.artezo.repository;

import com.artezo.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    // ==================== BASIC QUERIES ====================

    List<ReviewEntity> findByProductId(Long productId);
    List<ReviewEntity> findByUserId(Long userId);
    List<ReviewEntity> findByProductIdAndRating(Long productId, Integer rating);

    @Query("SELECT COUNT(r) > 0 FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.user.userId = :userId")
    boolean existsByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);

    // ==================== RATING AND COUNT QUERIES ====================

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.product.productPrimeId = :productId")
    Double getAverageRatingForProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.productPrimeId = :productId")
    Long getReviewCountForProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.imageData IS NOT NULL")
    long countByProductIdAndHasImage(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.videoData IS NOT NULL")
    long countByProductIdAndHasVideo(@Param("productId") Long productId);

    Long countByProductId(Long productId);

    void deleteByProductId(Long productId);

    // ==================== LIST REVIEWS QUERIES ====================

    @Query("SELECT r FROM ReviewEntity r WHERE r.product.productPrimeId = :productId ORDER BY r.createdAt DESC")
    List<ReviewEntity> findTopRecentReviews(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.imageData IS NOT NULL")
    List<ReviewEntity> findReviewsWithImages(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.videoData IS NOT NULL")
    List<ReviewEntity> findReviewsWithVideos(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.rating BETWEEN :minRating AND :maxRating")
    List<ReviewEntity> findByProductIdAndRatingBetween(@Param("productId") Long productId, @Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating);

    // ==================== RATING DISTRIBUTION ====================

    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.product.productPrimeId = :productId GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);

    // ==================== APPROVED REVIEWS QUERIES (PUBLIC API) ====================

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.approved = true")
    Double getAverageRatingForApprovedReviews(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.approved = true")
    Long getApprovedReviewCountForProduct(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.approved = true ORDER BY r.createdAt DESC")
    List<ReviewEntity> findTopRecentApprovedReviews(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.imageData IS NOT NULL AND r.approved = true")
    List<ReviewEntity> findApprovedReviewsWithImages(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.product.productPrimeId = :productId AND r.videoData IS NOT NULL AND r.approved = true")
    List<ReviewEntity> findApprovedReviewsWithVideos(@Param("productId") Long productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.approved = true ORDER BY r.createdAt DESC")
    List<ReviewEntity> findAllApprovedReviews();

    // ==================== ADMIN PANEL QUERIES ====================

    @Query("SELECT r FROM ReviewEntity r WHERE r.status = 'pending' ORDER BY r.createdAt DESC")
    List<ReviewEntity> findPendingReviews();

    @Query("SELECT r FROM ReviewEntity r WHERE r.flagged = true ORDER BY r.createdAt DESC")
    List<ReviewEntity> findFlaggedReviews();

    List<ReviewEntity> findByStatus(String status);
    List<ReviewEntity> findByApproved(Boolean approved);
    List<ReviewEntity> findByFlagged(Boolean flagged);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.status = 'pending'")
    Long countPendingReviews();

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.flagged = true")
    Long countFlaggedReviews();

    @Query("SELECT r FROM ReviewEntity r ORDER BY r.createdAt DESC")
    List<ReviewEntity> findAllOrderByCreatedAtDesc();

    // ==================== HELPER METHOD FOR EXISTING CHECK ====================

    // This method is kept for backward compatibility with existing service code
    default boolean existsByProductIdAndUserIdOld(Long productId, Long userId) {
        return existsByProductIdAndUserId(productId, userId);
    }
}

