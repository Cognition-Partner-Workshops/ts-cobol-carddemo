package com.aws.carddemo.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUserId(String userId);

    boolean existsByUserId(String userId);

    Page<AppUser> findByUserIdContainingIgnoreCase(String userId, Pageable pageable);
}
