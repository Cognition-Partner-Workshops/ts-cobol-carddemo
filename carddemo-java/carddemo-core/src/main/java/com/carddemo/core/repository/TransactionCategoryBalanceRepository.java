package com.carddemo.core.repository;

import com.carddemo.core.domain.TransactionCategoryBalance;
import com.carddemo.core.domain.TransactionCategoryBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TransactionCategoryBalance entity.
 * Replaces VSAM operations on TCATBALF file.
 * Used by interest calculation batch job (CBACT04C).
 */
@Repository
public interface TransactionCategoryBalanceRepository
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {

    List<TransactionCategoryBalance> findByAcctId(Long acctId);
}
