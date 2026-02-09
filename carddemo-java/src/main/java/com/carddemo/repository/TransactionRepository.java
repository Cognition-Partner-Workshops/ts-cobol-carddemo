package com.carddemo.repository;

import com.carddemo.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByCardNum(String cardNum, Pageable pageable);

    List<Transaction> findByCardNumOrderByOrigTsDesc(String cardNum);

    @Query("SELECT t FROM Transaction t WHERE t.cardNum IN " +
           "(SELECT x.cardNum FROM CardAccountXref x WHERE x.acctId = :acctId) " +
           "ORDER BY t.origTs DESC")
    Page<Transaction> findByAccountId(@Param("acctId") Long acctId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.cardNum = :cardNum " +
           "AND t.origTs BETWEEN :startDate AND :endDate ORDER BY t.origTs DESC")
    List<Transaction> findByCardNumAndDateRange(
            @Param("cardNum") String cardNum,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
