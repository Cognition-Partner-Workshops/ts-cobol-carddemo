package com.carddemo.transactiontype.repository;

import com.carddemo.transactiontype.entity.TransactionCategoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, Long> {
    List<TransactionCategoryBalance> findByAccountId(String accountId);
    
    List<TransactionCategoryBalance> findByCategoryCode(String categoryCode);
    
    Optional<TransactionCategoryBalance> findByAccountIdAndCategoryCodeAndBalanceDate(
        String accountId, String categoryCode, LocalDate balanceDate);
    
    @Query("SELECT b FROM TransactionCategoryBalance b WHERE b.accountId = :accountId AND b.balanceDate BETWEEN :startDate AND :endDate")
    List<TransactionCategoryBalance> findByAccountIdAndDateRange(
        @Param("accountId") String accountId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b FROM TransactionCategoryBalance b WHERE b.accountId = :accountId ORDER BY b.balanceDate DESC")
    List<TransactionCategoryBalance> findByAccountIdOrderByDateDesc(@Param("accountId") String accountId);
}
