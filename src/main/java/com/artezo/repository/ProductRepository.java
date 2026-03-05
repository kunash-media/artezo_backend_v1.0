package com.artezo.repository;

import com.artezo.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByProductStrId(String productStrId);

    boolean existsByProductStrId(String productStrId);

    Optional<Object> findByVariants_VariantId(String variantId);

    boolean existsByProductName(String productName);

    // Add this method inside ProductRepository interface
//    Optional<ProductEntity> findByIdWithInstallationSteps(Long productId);
}