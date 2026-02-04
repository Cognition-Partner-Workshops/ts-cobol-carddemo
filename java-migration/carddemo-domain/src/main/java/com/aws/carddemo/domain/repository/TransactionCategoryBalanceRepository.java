package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.TransactionCategoryBalance;
import com.aws.carddemo.domain.entity.TransactionCategoryBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {

    List<TransactionCategoryBalance> findByAccountAccountId(Long accountId);

    List<TransactionCategoryBalance> findByTransactionTypeCode(String typeCode);

    List<TransactionCategoryBalance> findByTransactionCategoryCode(Integer categoryCode);

    @Query("SELECT tcb FROM TransactionCategoryBalance tcb WHERE tcb.account.accountId = :accountId AND tcb.transactionTypeCode = :typeCode")
    List<TransactionCategoryBalance> findByAccountAndType(@Param("accountId") Long accountId, 
                                                           @Param("typeCode") String typeCode);

    @Query("SELECT SUM(tcb.balance) FROM TransactionCategoryBalance tcb WHERE tcb.account.accountId = :accountId")
    BigDecimal sumBalanceByAccount(@Param("accountId") Long accountId);

    @Query("SELECT SUM(tcb.balance) FROM TransactionCategoryBalance tcb WHERE tcb.transactionTypeCode = :typeCode")
    BigDecimal sumBalanceByType(@Param("typeCode") String typeCode);
}
