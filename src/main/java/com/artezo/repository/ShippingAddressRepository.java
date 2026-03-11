package com.artezo.repository;

import com.artezo.entity.ShippingAddressEntity;
import com.artezo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddressEntity, Long> {

    // All addresses for a user
    List<ShippingAddressEntity> findByUser(UserEntity user);

    // Get current default address
    Optional<ShippingAddressEntity> findByUserAndIsDefaultTrue(UserEntity user);

    // Unset all defaults for a user (used before setting a new default)
    @Modifying
    @Query("UPDATE ShippingAddressEntity s SET s.isDefault = false WHERE s.user = :user")
    void clearDefaultForUser(@Param("user") UserEntity user);

    // Count addresses for a user
    int countByUser(UserEntity user);
}