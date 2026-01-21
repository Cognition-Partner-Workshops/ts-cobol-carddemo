package com.carddemo.repository;

import com.carddemo.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for Transaction entity operations.
 * Provides CRUD operations and custom queries for transaction management.
 *
 * <p>Replaces mainframe VSAM file operations for TRANFILE.
 *
 * @see Transaction
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Find all transactions for a specific card.
     *
     * @param cardNumber the card number
     * @return list of transactions for the card
     */
    List<Transaction> findByCardNumber(String cardNumber);

    /**
     * Find all transactions for a specific card with pagination.
     *
     * @param cardNumber the card number
     * @param pageable pagination information
     * @return page of transactions for the card
     */
    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);

    /**
     * Find transactions by type code.
     *
     * @param typeCode the transaction type code
     * @return list of transactions with the specified type
     */
    List<Transaction> findByTypeCode(String typeCode);

    /**
     * Find transactions by category code.
     *
     * @param categoryCode the transaction category code
     * @return list of transactions with the specified category
     */
    List<Transaction> findByCategoryCode(Integer categoryCode);

    /**
     * Find transactions within a date range.
     *
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of transactions within the date range
     */
    List<Transaction> findByOriginationTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find transactions for a card within a date range.
     *
     * @param cardNumber the card number
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of transactions for the card within the date range
     */
    List<Transaction> findByCardNumberAndOriginationTimestampBetween(
            String cardNumber, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find transactions by merchant ID.
     *
     * @param merchantId the merchant identifier
     * @return list of transactions for the merchant
     */
    List<Transaction> findByMerchantId(Long merchantId);

    /**
     * Find transactions with amount greater than specified value.
     *
     * @param amount the minimum amount threshold
     * @return list of transactions above the threshold
     */
    List<Transaction> findByAmountGreaterThan(BigDecimal amount);

    /**
     * Calculate total transaction amount for a card.
     *
     * @param cardNumber the card number
     * @return total transaction amount
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.cardNumber = :cardNumber")
    BigDecimal sumAmountByCardNumber(@Param("cardNumber") String cardNumber);

    /**
     * Count transactions by type code.
     *
     * @param typeCode the transaction type code
     * @return count of transactions with the specified type
     */
    long countByTypeCode(String typeCode);

    /**
     * Find recent transactions for a card ordered by timestamp.
     *
     * @param cardNumber the card number
     * @param pageable pagination information
     * @return page of recent transactions
     */
    Page<Transaction> findByCardNumberOrderByOriginationTimestampDesc(String cardNumber, Pageable pageable);
}
