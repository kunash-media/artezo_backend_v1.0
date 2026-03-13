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

    @Query("SELECT COUNT(wi) FROM WishlistItemEntity wi WHERE wi.wishlist.userId = :userId")
    int countByWishlistUserId(@Param("userId") Long userId);

}