package com.artezo.repository;

import com.artezo.entity.InstallationStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallationStepRepository extends JpaRepository<InstallationStepEntity, Long> {

    List<InstallationStepEntity> findByProduct_ProductPrimeId(Long productId);

    InstallationStepEntity findByProduct_ProductPrimeIdAndStep(Long productId, int step);

    void deleteByProduct_ProductPrimeId(Long productPrimeId);
}