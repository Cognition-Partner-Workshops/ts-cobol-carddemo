package com.carddemo.transaction.repository;

import com.carddemo.common.entity.Transaction;
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

    @Query("SELECT t FROM Transaction t WHERE t.cardNumber = :cardNumber AND t.originationTimestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByCardNumberAndDateRange(
            @Param("cardNumber") String cardNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.originationTimestamp BETWEEN :startDate AND :endDate")
    Page<Transaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    List<Transaction> findByTransactionTypeCode(String transactionTypeCode);

    @Query("SELECT t FROM Transaction t WHERE t.merchantId = :merchantId")
    List<Transaction> findByMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.cardNumber = :cardNumber AND t.transactionTypeCode = :typeCode")
    BigDecimal sumAmountByCardAndType(@Param("cardNumber") String cardNumber, @Param("typeCode") String typeCode);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.originationTimestamp >= :since")
    Long countTransactionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.originationTimestamp >= :since AND t.amount > 0")
    BigDecimal sumDebitsSince(@Param("since") LocalDateTime since);

    @Query("SELECT SUM(ABS(t.amount)) FROM Transaction t WHERE t.originationTimestamp >= :since AND t.amount < 0")
    BigDecimal sumCreditsSince(@Param("since") LocalDateTime since);
}
