package com.aws.carddemo.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRecordRepository
        extends JpaRepository<TransactionRecord, Long>, JpaSpecificationExecutor<TransactionRecord> {

    List<TransactionRecord> findByCardCardNumber(String cardNumber);

    Page<TransactionRecord> findByCardCardNumberOrderByTimestampDesc(String cardNumber, Pageable pageable);

    @Query("SELECT t FROM TransactionRecord t WHERE t.card.cardNumber = :cardNumber "
            + "AND t.timestamp BETWEEN :fromDate AND :toDate ORDER BY t.timestamp DESC")
    Page<TransactionRecord> findByCardNumberAndDateRange(
            @Param("cardNumber") String cardNumber,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT t FROM TransactionRecord t WHERE t.card.cardNumber = :cardNumber "
            + "AND t.transactionType = :typeCode "
            + "AND t.amount BETWEEN :minAmount AND :maxAmount ORDER BY t.timestamp DESC")
    Page<TransactionRecord> findByCardNumberAndTypeAndAmountRange(
            @Param("cardNumber") String cardNumber,
            @Param("typeCode") String typeCode,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
