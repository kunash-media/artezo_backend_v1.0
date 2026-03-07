package com.artezo.repository;

import com.artezo.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByProductStrId(String productStrId);

    boolean existsByProductStrId(String productStrId);

    Optional<Object> findByVariants_VariantId(String variantId);

    boolean existsByProductName(String productName);

    // Add this method inside ProductRepository interface
    // Optional<ProductEntity> findByIdWithInstallationSteps(Long productId);


    @Query("SELECT p FROM ProductEntity p WHERE p.isDeleted = false")
    Page<ProductEntity> findAllActiveProducts(Pageable pageable);

    // Alternative: Fixed sort (if you want to force descending ID always)
    @Query("SELECT p FROM ProductEntity p WHERE p.isDeleted = false ORDER BY p.productPrimeId DESC")
    Page<ProductEntity> findAllActiveProductsOrdered(Pageable pageable);

    // Count of active products (useful for stats, admin dashboard, etc.)
    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.isDeleted = false")
    long countActiveProducts();

    @Query(value = "SELECT * FROM product WHERE is_deleted = false", nativeQuery = true)
    Page<ProductEntity> findAllActiveNative(Pageable pageable);

}