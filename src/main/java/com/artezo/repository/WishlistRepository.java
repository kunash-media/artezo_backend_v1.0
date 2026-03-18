package com.artezo.repository;

import com.artezo.entity.WishlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistEntity, Long> {

    List<WishlistEntity> findByUser_UserId(Long userId);

    Optional<WishlistEntity> findByUser_UserIdAndName(Long userId, String name);

}