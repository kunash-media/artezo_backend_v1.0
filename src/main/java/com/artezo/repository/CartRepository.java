package com.artezo.repository;

import com.artezo.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUser_UserIdAndStatus(Long userId, CartEntity.CartStatus status);

    Optional<CartEntity> findBySessionIdAndStatus(String sessionId, CartEntity.CartStatus status);
}