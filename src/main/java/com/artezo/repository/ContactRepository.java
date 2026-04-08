package com.artezo.repository;


import com.artezo.entity.ContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<ContactEntity, Long> {

    List<ContactEntity> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    // Optional: Check by email and phone combination
    Optional<ContactEntity> findByEmailAndPhoneAndCreatedAtAfter(String email, String phone, LocalDateTime createdAt);

    Optional<ContactEntity> findByEmailAndCreatedAtAfter(String email, LocalDateTime twentyFourHoursAgo);
}
