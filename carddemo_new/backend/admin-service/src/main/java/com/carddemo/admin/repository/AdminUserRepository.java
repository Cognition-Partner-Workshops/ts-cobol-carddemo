package com.carddemo.admin.repository;

import com.carddemo.admin.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {
    Page<AdminUser> findByUserType(String userType, Pageable pageable);
    Page<AdminUser> findByActive(boolean active, Pageable pageable);
    Page<AdminUser> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);
    boolean existsByUserId(String userId);
}
