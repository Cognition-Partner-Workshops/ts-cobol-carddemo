package com.carddemo.repository;

import com.carddemo.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for Account entities.
 * Replaces VSAM KSDS I/O operations on ACCTFILE.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}
