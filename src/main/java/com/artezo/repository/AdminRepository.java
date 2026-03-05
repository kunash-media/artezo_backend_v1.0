package com.artezo.repository;


import com.artezo.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<AdminEntity, Long> {

    boolean existsByAdminId(String candidate);
}
