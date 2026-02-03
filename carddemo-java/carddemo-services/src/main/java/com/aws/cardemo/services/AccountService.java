package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Account;
import com.aws.cardemo.persistence.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing credit card Account business logic.
 * 
 * This service provides the business layer for account operations in the CardDemo application.
 * It encapsulates all account-related business rules and coordinates with the persistence layer
 * through the AccountRepository. All methods are transactional to ensure data consistency.
 * 
 * Key responsibilities include:
 * - CRUD operations for account management
 * - Balance updates for credit and debit transactions
 * - Credit limit monitoring and validation
 * - Account status and group filtering
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Retrieves all accounts from the database.
     * 
     * @return List of all Account entities in the system
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Retrieves an account by its unique identifier.
     * 
     * @param accountId The unique account identifier (11 characters max)
     * @return Optional containing the Account if found, empty Optional otherwise
     */
    public Optional<Account> getAccountById(String accountId) {
        return accountRepository.findById(accountId);
    }

    /**
     * Retrieves an active account by its unique identifier.
     * 
     * Only returns accounts with status 'A' (Active).
     * 
     * @param accountId The unique account identifier
     * @return Optional containing the active Account if found, empty Optional otherwise
     */
    public Optional<Account> getActiveAccountById(String accountId) {
        return accountRepository.findActiveAccountById(accountId);
    }

    /**
     * Creates a new account in the system.
     * 
     * @param account The Account entity to create
     * @return The created Account with any generated values populated
     */
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    /**
     * Updates an existing account in the system.
     * 
     * @param account The Account entity with updated values
     * @return The updated Account entity
     */
    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }

    /**
     * Deletes an account from the system.
     * 
     * This is a hard delete operation. Consider implementing soft delete for production use.
     * 
     * @param accountId The unique identifier of the account to delete
     */
    public void deleteAccount(String accountId) {
        accountRepository.deleteById(accountId);
    }

    /**
     * Retrieves all accounts with a specific status.
     * 
     * @param status The account status code (A=Active, C=Closed, S=Suspended)
     * @return List of accounts matching the specified status
     */
    public List<Account> getAccountsByStatus(String status) {
        return accountRepository.findByAccountStatus(status);
    }

    /**
     * Retrieves all accounts belonging to a specific group.
     * 
     * @param groupId The group identifier for filtering
     * @return List of accounts in the specified group
     */
    public List<Account> getAccountsByGroupId(String groupId) {
        return accountRepository.findByGroupId(groupId);
    }

    /**
     * Retrieves all accounts that have exceeded their credit limit.
     * 
     * This is useful for risk management and collections processes.
     * 
     * @return List of accounts where current balance exceeds credit limit
     */
    public List<Account> getAccountsOverCreditLimit() {
        return accountRepository.findAccountsOverCreditLimit();
    }

    /**
     * Updates the balance of an account based on a transaction.
     * 
     * For credit transactions (payments), the balance is reduced and cycle credit is increased.
     * For debit transactions (purchases), the balance is increased and cycle debit is increased.
     * 
     * @param accountId The unique identifier of the account to update
     * @param amount The transaction amount (must be positive)
     * @param isCredit True for credit/payment transactions, false for debit/purchase transactions
     * @return The updated Account entity
     * @throws IllegalArgumentException if the account is not found
     */
    public Account updateBalance(String accountId, BigDecimal amount, boolean isCredit) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        BigDecimal currentBalance = account.getCurrentBalance();
        if (isCredit) {
            account.setCurrentBalance(currentBalance.subtract(amount));
            account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(amount));
        } else {
            account.setCurrentBalance(currentBalance.add(amount));
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(amount));
        }

        return accountRepository.save(account);
    }

    /**
     * Checks if an account has exceeded its credit limit.
     * 
     * @param accountId The unique identifier of the account to check
     * @return True if the current balance exceeds the credit limit, false otherwise or if account not found
     */
    public boolean isAccountOverCreditLimit(String accountId) {
        return accountRepository.findById(accountId)
                .map(account -> account.getCurrentBalance().compareTo(account.getCreditLimit()) > 0)
                .orElse(false);
    }
}
