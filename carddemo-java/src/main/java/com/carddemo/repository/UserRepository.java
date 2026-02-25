package com.carddemo.repository;

import com.carddemo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity (VSAM file USRSEC).
 * Replaces CICS READ/WRITE/REWRITE/DELETE on USRSEC dataset.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);

    Page<User> findByUserType(String userType, Pageable pageable);

    boolean existsByUserId(String userId);
}
