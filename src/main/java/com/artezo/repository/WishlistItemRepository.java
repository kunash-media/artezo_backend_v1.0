package com.artezo.repository;

import com.artezo.entity.WishlistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, Long> {

    List<WishlistItemEntity> findByWishlist_Id(Long wishlistId);

    Optional<WishlistItemEntity> findByWishlist_IdAndProductIdAndVariantId(Long wishlistId, Long productId, String variantId);

    boolean existsByWishlist_IdAndProductIdAndVariantId(Long wishlistId, Long productId, String variantId);

    void deleteByWishlist_IdAndProductIdAndVariantId(Long wishlistId, Long productId, String variantId);

    @Query("SELECT COUNT(wi) FROM WishlistItemEntity wi WHERE wi.wishlist.user.userId = :userId")
    int countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(wi) > 0 FROM WishlistItemEntity wi " +
            "WHERE wi.wishlist.user.userId = :userId " +
            "AND wi.productId = :productId " +
            "AND (wi.variantId = :variantId OR (wi.variantId IS NULL AND :variantId IS NULL))")
    boolean existsByUserIdAndProductIdAndVariantId(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("variantId") String variantId);

    @Query("SELECT COUNT(wi) > 0 FROM WishlistItemEntity wi " +
            "WHERE wi.wishlist.user.userId = :userId " +
            "AND wi.productId = :productId")
    boolean existsByUserIdAndProductId(
            @Param("userId") Long userId,
            @Param("productId") Long productId);
}