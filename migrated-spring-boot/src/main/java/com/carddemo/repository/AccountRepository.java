package com.carddemo.repository;

import com.carddemo.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * JPA repository for Account entity operations.
 * Provides CRUD operations and custom queries for account management.
 *
 * <p>Replaces mainframe VSAM file operations for ACCTFILE.
 *
 * @see Account
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find all accounts with a specific active status.
     *
     * @param activeStatus the status to filter by ('Y' for active, 'N' for inactive)
     * @return list of accounts matching the status
     */
    List<Account> findByActiveStatus(String activeStatus);

    /**
     * Find all accounts with a specific active status with pagination.
     *
     * @param activeStatus the status to filter by
     * @param pageable pagination information
     * @return page of accounts matching the status
     */
    Page<Account> findByActiveStatus(String activeStatus, Pageable pageable);

    /**
     * Find accounts by group ID.
     *
     * @param groupId the group identifier
     * @return list of accounts in the specified group
     */
    List<Account> findByGroupId(String groupId);

    /**
     * Find accounts with balance exceeding credit limit.
     *
     * @return list of accounts over their credit limit
     */
    @Query("SELECT a FROM Account a WHERE a.currentBalance > a.creditLimit")
    List<Account> findAccountsOverCreditLimit();

    /**
     * Find accounts by ZIP code.
     *
     * @param addressZip the ZIP code to search for
     * @return list of accounts matching the ZIP code
     */
    List<Account> findByAddressZip(String addressZip);

    /**
     * Find accounts with balance greater than specified amount.
     *
     * @param balance the minimum balance threshold
     * @return list of accounts with balance above threshold
     */
    List<Account> findByCurrentBalanceGreaterThan(BigDecimal balance);

    /**
     * Count accounts by active status.
     *
     * @param activeStatus the status to count
     * @return count of accounts with the specified status
     */
    long countByActiveStatus(String activeStatus);
}
