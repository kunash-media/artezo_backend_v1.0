package com.artezo.repository;

import com.artezo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    UserEntity getByEmail(String email);

    Optional<UserEntity> findByUserId(Long userId);


    List<UserEntity> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

}