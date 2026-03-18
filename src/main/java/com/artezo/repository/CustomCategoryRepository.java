package com.artezo.repository;

import com.artezo.entity.CustomCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomCategoryRepository extends JpaRepository<CustomCategoryEntity, Long> {

    boolean existsByProductCategoryIgnoreCase(String productCategory);

    Optional<CustomCategoryEntity> findByProductCategoryIgnoreCase(String productCategory);
}