package com.aws.carddemo.repository;

import com.aws.carddemo.entity.Transaction;
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

    List<Transaction> findByCardCardNum(String cardNum);

    Page<Transaction> findByCardCardNum(String cardNum, Pageable pageable);

    List<Transaction> findByTranTypeCd(String typeCd);

    @Query("SELECT t FROM Transaction t WHERE t.tranOrigTs BETWEEN :startDate AND :endDate")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.card.cardNum = :cardNum AND t.tranOrigTs BETWEEN :startDate AND :endDate")
    List<Transaction> findByCardAndDateRange(@Param("cardNum") String cardNum,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.tranMerchantId = :merchantId")
    List<Transaction> findByMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT SUM(t.tranAmt) FROM Transaction t WHERE t.card.cardNum = :cardNum")
    BigDecimal sumTransactionsByCard(@Param("cardNum") String cardNum);

    @Query("SELECT SUM(t.tranAmt) FROM Transaction t WHERE t.card.account.acctId = :acctId")
    BigDecimal sumTransactionsByAccount(@Param("acctId") Long acctId);

    @Query("SELECT t FROM Transaction t WHERE t.tranAmt > :amount")
    List<Transaction> findLargeTransactions(@Param("amount") BigDecimal amount);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.card.cardNum = :cardNum")
    long countByCard(@Param("cardNum") String cardNum);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.card WHERE t.tranId = :tranId")
    Transaction findByIdWithCard(@Param("tranId") String tranId);
}
