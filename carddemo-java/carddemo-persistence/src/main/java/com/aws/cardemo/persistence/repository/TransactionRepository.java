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

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByCardNumber(String cardNumber);

    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);

    List<Transaction> findByTransactionTypeCode(String typeCode);

    @Query("SELECT t FROM Transaction t WHERE t.cardNumber = :cardNumber AND t.transactionTimestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByCardNumberAndDateRange(
            @Param("cardNumber") String cardNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.merchantId = :merchantId")
    List<Transaction> findByMerchantId(@Param("merchantId") String merchantId);

    @Query("SELECT t FROM Transaction t WHERE t.transactionTimestamp >= :since ORDER BY t.transactionTimestamp DESC")
    List<Transaction> findRecentTransactions(@Param("since") LocalDateTime since);
}
