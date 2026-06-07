// com/artezo/repository/ProductVariantRepository.java
package com.artezo.repository;

import com.artezo.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {
}