package com.artezo.repository;

import com.artezo.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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


    //-----------------------------------------------------------//
    // Suggestion query — Phase 2 (Redis powered, personalised)  //
    //-----------------------------------------------------------//
    @Query("SELECT p FROM ProductEntity p WHERE " +
            "(p.productCategory IN :categories OR p.productSubCategory IN :subCategories) " +
            "AND p.productPrimeId NOT IN :excludedIds " +
            "AND p.isDeleted = false " +
            "AND p.currentStock > 0 " +
            "ORDER BY p.underTrendCategory DESC, p.currentStock DESC")
    List<ProductEntity> findSuggestions(
            @Param("categories") List<String> categories,
            @Param("subCategories") List<String> subCategories,
            @Param("excludedIds") List<Long> excludedIds,
            Pageable pageable
    );

    // Fallback query — Phase 1 (no Redis data, new user)
    @Query("SELECT p FROM ProductEntity p WHERE " +
            "(p.productCategory = :category OR p.productSubCategory = :subCategory) " +
            "AND p.productPrimeId != :excludeId " +
            "AND p.isDeleted = false " +
            "AND p.currentStock > 0 " +
            "ORDER BY p.underTrendCategory DESC, p.currentStock DESC")
    List<ProductEntity> findSuggestionsFallback(
            @Param("category") String category,
            @Param("subCategory") String subCategory,
            @Param("excludeId") Long excludeId,
            Pageable pageable
    );

    boolean existsByCurrentSku(String sku);

    boolean existsByHsnCode(String hsnCode);


    // ProductRepository.java

    @Query("SELECT p FROM ProductEntity p WHERE p.productCategory = :category AND p.isDeleted = false")
    Page<ProductEntity> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM ProductEntity p WHERE p.productSubCategory = :subCategory AND p.isDeleted = false")
    Page<ProductEntity> findBySubCategory(@Param("subCategory") String subCategory, Pageable pageable);


    @Query(
            value = "SELECT * FROM products_table WHERE is_deleted = false AND JSON_CONTAINS(addon_keys, JSON_QUOTE(:addonKey))",
            countQuery = "SELECT COUNT(*) FROM products_table WHERE is_deleted = false AND JSON_CONTAINS(addon_keys, JSON_QUOTE(:addonKey))",
            nativeQuery = true
    )
    Page<ProductEntity> findByAddonKey(@Param("addonKey") String addonKey, Pageable pageable);

    @Query(
            value = "SELECT * FROM products_table WHERE is_deleted = false AND JSON_CONTAINS(global_tags, JSON_QUOTE(:tag))",
            countQuery = "SELECT COUNT(*) FROM products_table WHERE is_deleted = false AND JSON_CONTAINS(global_tags, JSON_QUOTE(:tag))",
            nativeQuery = true
    )
    Page<ProductEntity> findByGlobalTag(@Param("tag") String tag, Pageable pageable);
}