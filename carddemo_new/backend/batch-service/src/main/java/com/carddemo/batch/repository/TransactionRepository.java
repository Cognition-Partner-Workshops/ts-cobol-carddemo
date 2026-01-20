package com.carddemo.batch.repository;

import com.carddemo.batch.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByAccountId(String accountId);
    
    List<Transaction> findByStatus(String status);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' ORDER BY t.originalTimestamp")
    List<Transaction> findPendingTransactions();
    
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.originalTimestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByAccountIdAndDateRange(
        @Param("accountId") String accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t FROM Transaction t WHERE t.status = 'POSTED' AND t.postedTimestamp >= :date")
    List<Transaction> findPostedTransactionsSince(@Param("date") LocalDateTime date);
}
