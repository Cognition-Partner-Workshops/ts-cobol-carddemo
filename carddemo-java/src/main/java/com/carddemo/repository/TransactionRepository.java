package com.carddemo.repository;

import com.carddemo.entity.Transaction;
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

    Page<Transaction> findByCardNum(String cardNum, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.cardNum IN :cardNums")
    Page<Transaction> findByCardNumIn(@Param("cardNums") List<String> cardNums, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.origTs BETWEEN :startDate AND :endDate ORDER BY t.origTs")
    List<Transaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t JOIN CardCrossReference x ON t.cardNum = x.cardNum WHERE x.acctId = :acctId ORDER BY t.origTs DESC")
    Page<Transaction> findByAccountId(@Param("acctId") Long acctId, Pageable pageable);
}
