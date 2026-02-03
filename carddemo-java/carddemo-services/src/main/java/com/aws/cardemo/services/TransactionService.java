package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Transaction;
import com.aws.cardemo.persistence.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing credit card Transaction business logic.
 * 
 * This service provides the business layer for transaction operations in the CardDemo application.
 * It encapsulates all transaction-related business rules and coordinates with the persistence layer
 * through the TransactionRepository. All methods are transactional to ensure data consistency.
 * 
 * Key responsibilities include:
 * - CRUD operations for transaction management
 * - Transaction history retrieval with pagination support
 * - Date range filtering for statement generation
 * - Merchant-based transaction queries
 * - Real-time transaction monitoring
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Retrieves all transactions from the database.
     * 
     * @return List of all Transaction entities in the system
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Retrieves a transaction by its unique identifier.
     * 
     * @param transactionId The unique transaction identifier (16 characters max)
     * @return Optional containing the Transaction if found, empty Optional otherwise
     */
    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    /**
     * Creates a new transaction in the system.
     * 
     * If no timestamp is provided, the current timestamp will be set automatically.
     * 
     * @param transaction The Transaction entity to create
     * @return The created Transaction with any generated values populated
     */
    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getTransactionTimestamp() == null) {
            transaction.setTransactionTimestamp(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    /**
     * Updates an existing transaction in the system.
     * 
     * Note: In production, transactions are typically immutable. This method is
     * provided for administrative corrections only.
     * 
     * @param transaction The Transaction entity with updated values
     * @return The updated Transaction entity
     */
    public Transaction updateTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    /**
     * Deletes a transaction from the system.
     * 
     * This is a hard delete operation. In production, consider implementing soft delete
     * or archiving for audit trail purposes.
     * 
     * @param transactionId The unique identifier of the transaction to delete
     */
    public void deleteTransaction(String transactionId) {
        transactionRepository.deleteById(transactionId);
    }

    /**
     * Retrieves all transactions for a specific card.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @return List of transactions for the specified card
     */
    public List<Transaction> getTransactionsByCardNumber(String cardNumber) {
        return transactionRepository.findByCardNumber(cardNumber);
    }

    /**
     * Retrieves transactions for a specific card with pagination support.
     * 
     * This method is recommended for cards with large transaction histories
     * to improve performance and reduce memory usage.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of transactions for the specified card
     */
    public Page<Transaction> getTransactionsByCardNumber(String cardNumber, Pageable pageable) {
        return transactionRepository.findByCardNumber(cardNumber, pageable);
    }

    /**
     * Retrieves all transactions of a specific type.
     * 
     * @param typeCode The transaction type code (e.g., 'PUR' for purchase, 'PMT' for payment)
     * @return List of transactions matching the specified type
     */
    public List<Transaction> getTransactionsByTypeCode(String typeCode) {
        return transactionRepository.findByTransactionTypeCode(typeCode);
    }

    /**
     * Retrieves transactions for a specific card within a date range.
     * 
     * This method is useful for statement generation and period-specific reporting.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @param startDate The start of the date range (inclusive)
     * @param endDate The end of the date range (inclusive)
     * @return List of transactions within the specified date range
     */
    public List<Transaction> getTransactionsByCardNumberAndDateRange(
            String cardNumber, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByCardNumberAndDateRange(cardNumber, startDate, endDate);
    }

    /**
     * Retrieves all transactions for a specific merchant.
     * 
     * This method is useful for merchant reconciliation and reporting.
     * 
     * @param merchantId The merchant identifier to filter transactions by
     * @return List of transactions for the specified merchant
     */
    public List<Transaction> getTransactionsByMerchantId(String merchantId) {
        return transactionRepository.findByMerchantId(merchantId);
    }

    /**
     * Retrieves transactions that occurred after a specified timestamp.
     * 
     * @param since The timestamp to filter transactions from
     * @return List of transactions that occurred after the specified time
     */
    public List<Transaction> getRecentTransactions(LocalDateTime since) {
        return transactionRepository.findRecentTransactions(since);
    }

    /**
     * Retrieves all transactions from the last 24 hours.
     * 
     * This method is useful for real-time monitoring, fraud detection,
     * and daily activity reports.
     * 
     * @return List of transactions that occurred in the last 24 hours
     */
    public List<Transaction> getTransactionsLast24Hours() {
        return transactionRepository.findRecentTransactions(LocalDateTime.now().minusHours(24));
    }
}
