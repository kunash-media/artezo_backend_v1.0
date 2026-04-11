package com.artezo.repository;

import com.artezo.entity.VariantMockupImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariantMockupImageRepository extends JpaRepository<VariantMockupImageEntity, Long> {
    void deleteByVariant_Id(Long variantId);
    List<VariantMockupImageEntity> findByVariant_Id(Long variantId);
}
