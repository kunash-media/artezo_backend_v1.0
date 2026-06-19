package com.artezo.repository;

import com.artezo.entity.CheckoutUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckoutUserRepository extends JpaRepository<CheckoutUserEntity, Long> {
    Optional<CheckoutUserEntity> findByPhone(String phone);
}