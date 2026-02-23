package com.carddemo.core.repository;

import com.carddemo.core.domain.UserSecurity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserSecurity entity.
 * Replaces VSAM READ/WRITE/REWRITE/DELETE operations on USRSEC file.
 * VSAM key: SEC-USR-ID (PIC X(08))
 */
@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, String> {

    Optional<UserSecurity> findByUsrId(String usrId);

    Page<UserSecurity> findByUsrType(String usrType, Pageable pageable);

    boolean existsByUsrId(String usrId);
}
