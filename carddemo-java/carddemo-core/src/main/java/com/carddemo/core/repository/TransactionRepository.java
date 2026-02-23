package com.carddemo.core.repository;

import com.carddemo.core.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Transaction entity.
 * Replaces VSAM READ/WRITE/REWRITE/DELETE operations on TRANSACT file.
 * VSAM key: TRAN-ID (PIC X(16))
 * VSAM AIX equivalents: by card number, by timestamp
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByCardNum(String cardNum, Pageable pageable);

    @Query("SELECT t FROM Transaction t JOIN CardXref x ON t.cardNum = x.cardNum " +
            "WHERE x.acctId = :acctId")
    Page<Transaction> findByAccountId(@Param("acctId") Long acctId, Pageable pageable);

    Page<Transaction> findByOrigTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.cardNum = :cardNum " +
            "AND t.origTimestamp BETWEEN :start AND :end")
    Page<Transaction> findByCardNumAndDateRange(
            @Param("cardNum") String cardNum,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    List<Transaction> findByOrigTimestampBetweenOrderByOrigTimestampAsc(
            LocalDateTime start, LocalDateTime end);
}
