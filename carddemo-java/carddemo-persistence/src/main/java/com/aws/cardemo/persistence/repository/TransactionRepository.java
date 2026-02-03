package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA Repository for Transaction entity persistence operations.
 * 
 * This repository provides data access methods for credit card transactions in the CardDemo application.
 * It extends JpaRepository to inherit standard CRUD operations and adds custom query methods
 * for transaction-specific business requirements.
 * 
 * The repository supports:
 * - Standard CRUD operations (inherited from JpaRepository)
 * - Transaction filtering by card number with pagination
 * - Date range queries for statement generation
 * - Merchant-based transaction lookups
 * - Recent transaction queries for monitoring
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Finds all transactions for a specific card.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @return List of transactions for the specified card
     */
    List<Transaction> findByCardNumber(String cardNumber);

    /**
     * Finds transactions for a specific card with pagination support.
     * 
     * This method is recommended for cards with large transaction histories
     * to improve performance and reduce memory usage.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of transactions for the specified card
     */
    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);

    /**
     * Finds all transactions of a specific type.
     * 
     * @param typeCode The transaction type code (e.g., 'PUR' for purchase, 'PMT' for payment)
     * @return List of transactions matching the specified type
     */
    List<Transaction> findByTransactionTypeCode(String typeCode);

    /**
     * Finds transactions for a specific card within a date range.
     * 
     * This query is useful for statement generation and period-specific reporting.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @param startDate The start of the date range (inclusive)
     * @param endDate The end of the date range (inclusive)
     * @return List of transactions within the specified date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.cardNumber = :cardNumber AND t.transactionTimestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByCardNumberAndDateRange(
            @Param("cardNumber") String cardNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Finds all transactions for a specific merchant.
     * 
     * This query is useful for merchant reconciliation and reporting.
     * 
     * @param merchantId The merchant identifier to filter transactions by
     * @return List of transactions for the specified merchant
     */
    @Query("SELECT t FROM Transaction t WHERE t.merchantId = :merchantId")
    List<Transaction> findByMerchantId(@Param("merchantId") String merchantId);

    /**
     * Finds transactions that occurred after a specified timestamp.
     * 
     * Results are ordered by transaction timestamp in descending order (most recent first).
     * This query is useful for real-time monitoring and fraud detection.
     * 
     * @param since The timestamp to filter transactions from
     * @return List of transactions that occurred after the specified time
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionTimestamp >= :since ORDER BY t.transactionTimestamp DESC")
    List<Transaction> findRecentTransactions(@Param("since") LocalDateTime since);
}
