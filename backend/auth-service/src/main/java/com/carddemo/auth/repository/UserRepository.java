package com.carddemo.auth.repository;

import com.carddemo.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByUserIdAndIsActiveTrue(String userId);
    
    boolean existsByUserId(String userId);
}
