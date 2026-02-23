package com.carddemo.core.repository;

import com.carddemo.core.domain.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Account entity.
 * Replaces VSAM READ/WRITE/REWRITE/DELETE operations on ACCTDATA file.
 * VSAM key: ACCT-ID (PIC 9(11))
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Page<Account> findByActiveStatus(String activeStatus, Pageable pageable);

    List<Account> findByGroupId(String groupId);

    @Query("SELECT a FROM Account a WHERE a.acctId >= :startId ORDER BY a.acctId ASC")
    Page<Account> findByAcctIdGreaterThanEqual(@Param("startId") Long startId, Pageable pageable);
}
