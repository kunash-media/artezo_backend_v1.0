package com.artezo.repository;

import com.artezo.entity.InstallationStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstallationStepRepository extends JpaRepository<InstallationStepEntity, Long> {

    List<InstallationStepEntity> findByProduct_ProductPrimeId(Long productPrimeId);

    InstallationStepEntity findByProduct_ProductPrimeIdAndStep(Long productPrimeId, int step);

    void deleteByProduct_ProductPrimeId(Long productPrimeId);

    Optional<InstallationStepEntity> findByProduct_ProductPrimeIdAndStep(Long productPrimeId, Integer step);
}