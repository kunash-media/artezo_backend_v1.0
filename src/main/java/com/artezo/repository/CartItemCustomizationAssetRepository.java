// ── CartItemCustomizationAssetRepository.java ─────────────────────────────
package com.artezo.repository;

import com.artezo.entity.CartItemCustomizationAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CartItemCustomizationAssetRepository
        extends JpaRepository<CartItemCustomizationAssetEntity, Long> {

    List<CartItemCustomizationAssetEntity> findByCartItem_IdOrderBySlotNumberAsc(
            Long cartItemId);

    @Modifying
    @Query("DELETE FROM CartItemCustomizationAssetEntity c " +
            "WHERE c.cartItem.id = :cartItemId")
    void deleteAllByCartItemId(@Param("cartItemId") Long cartItemId);
}