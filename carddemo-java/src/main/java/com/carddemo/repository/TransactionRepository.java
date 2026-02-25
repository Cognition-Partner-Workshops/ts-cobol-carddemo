package com.carddemo.repository;

import com.carddemo.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for Transaction entity (VSAM file TRANSACT).
 * Replaces CICS READ/WRITE/STARTBR/READNEXT/READPREV on TRANSACT dataset.
 * AIX on CARD-NUM replaced by DB index.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByCardNum(String cardNum, Pageable pageable);

    List<Transaction> findByCardNumOrderByOrigTimestampDesc(String cardNum);

    Page<Transaction> findByTranTypeCd(String tranTypeCd, Pageable pageable);

    @Query("SELECT SUM(t.tranAmt) FROM Transaction t WHERE t.cardNum = :cardNum")
    BigDecimal sumAmountByCardNum(@Param("cardNum") String cardNum);

    @Query("SELECT t FROM Transaction t WHERE t.cardNum = :cardNum AND t.tranTypeCd = :typeCd")
    List<Transaction> findByCardNumAndType(@Param("cardNum") String cardNum, @Param("typeCd") String typeCd);
}
