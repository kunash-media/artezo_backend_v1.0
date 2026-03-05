package com.artezo.repository;


import com.artezo.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findBySku(String sku);

    // For bulk operations if needed later
    List<InventoryEntity> findBySkuIn(List<String> skus);
}