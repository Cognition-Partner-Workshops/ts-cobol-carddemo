package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByCardNumber(String cardNumber);

    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);

    List<Transaction> findByTransactionTypeCode(String typeCode);

    Page<Transaction> findByTransactionTypeCode(String typeCode, Pageable pageable);

    List<Transaction> findByTransactionCategoryCode(Integer categoryCode);

    Page<Transaction> findByTransactionCategoryCode(Integer categoryCode, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.originTimestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.originTimestamp BETWEEN :startDate AND :endDate")
    Page<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.cardNumber = :cardNumber AND t.originTimestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByCardNumberAndDateRange(@Param("cardNumber") String cardNumber,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.merchantId = :merchantId")
    List<Transaction> findByMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT t FROM Transaction t WHERE t.merchantName LIKE %:merchantName%")
    Page<Transaction> findByMerchantNameContaining(@Param("merchantName") String merchantName, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.cardNumber = :cardNumber AND t.originTimestamp BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByCardNumberAndDateRange(@Param("cardNumber") String cardNumber,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionTypeCode = :typeCode AND t.originTimestamp BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTypeAndDateRange(@Param("typeCode") String typeCode,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.cardNumber = :cardNumber")
    long countByCardNumber(@Param("cardNumber") String cardNumber);

    @Query("SELECT t FROM Transaction t WHERE t.processTimestamp IS NULL")
    List<Transaction> findUnprocessedTransactions();

    @Query("SELECT t FROM Transaction t WHERE t.amount > :amount")
    List<Transaction> findLargeTransactions(@Param("amount") BigDecimal amount);

    @Query("SELECT t.transactionTypeCode, COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.originTimestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY t.transactionTypeCode")
    List<Object[]> getTransactionSummaryByType(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
}
