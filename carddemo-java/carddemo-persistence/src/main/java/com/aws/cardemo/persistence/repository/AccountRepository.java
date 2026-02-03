package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Account entity persistence operations.
 * 
 * This repository provides data access methods for credit card accounts in the CardDemo application.
 * It extends JpaRepository to inherit standard CRUD operations and adds custom query methods
 * for account-specific business requirements.
 * 
 * The repository supports:
 * - Standard CRUD operations (inherited from JpaRepository)
 * - Account filtering by status and group
 * - Credit limit monitoring queries
 * - Active account lookups
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Finds all accounts with a specific status.
     * 
     * @param status The account status code (A=Active, C=Closed, S=Suspended)
     * @return List of accounts matching the specified status
     */
    List<Account> findByAccountStatus(String status);

    /**
     * Finds all accounts belonging to a specific group.
     * 
     * @param groupId The group identifier for filtering
     * @return List of accounts in the specified group
     */
    List<Account> findByGroupId(String groupId);

    /**
     * Finds all accounts where the current balance exceeds the credit limit.
     * 
     * This query is useful for risk management and collections processes.
     * 
     * @return List of accounts that are over their credit limit
     */
    @Query("SELECT a FROM Account a WHERE a.currentBalance > a.creditLimit")
    List<Account> findAccountsOverCreditLimit();

    /**
     * Finds an active account by its unique identifier.
     * 
     * Only returns accounts with status 'A' (Active).
     * 
     * @param accountId The unique account identifier
     * @return Optional containing the active Account if found, empty Optional otherwise
     */
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId AND a.accountStatus = 'A'")
    Optional<Account> findActiveAccountById(@Param("accountId") String accountId);
}
