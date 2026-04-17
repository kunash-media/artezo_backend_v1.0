package com.artezo.repository;

import com.artezo.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCart_Id(Long cartId);

    Optional<CartItemEntity> findByCart_IdAndProductIdAndVariantId(Long cartId, Long productId, String variantId);

    void deleteByCart_IdAndProductIdAndVariantId(Long cartId, Long productId, String variantId);

    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItemEntity ci WHERE ci.cart.id = :cartId")
    Integer sumQuantityByCartId(@Param("cartId") Long cartId);

    // ADD THIS NEW METHOD
    @Query("SELECT COUNT(ci) FROM CartItemEntity ci WHERE ci.cart.id = :cartId")
    Integer countByCartId(@Param("cartId") Long cartId);

}