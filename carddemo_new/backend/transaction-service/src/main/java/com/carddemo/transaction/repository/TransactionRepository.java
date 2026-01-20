package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.Transaction;
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
    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);
    Page<Transaction> findByAccountId(String accountId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.originalTimestamp BETWEEN :startDate AND :endDate")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    List<Transaction> findByAccountIdAndStatus(String accountId, String status);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.accountId = :accountId AND t.typeCode = :typeCode")
    BigDecimal sumAmountByAccountIdAndTypeCode(@Param("accountId") String accountId, @Param("typeCode") String typeCode);
}
