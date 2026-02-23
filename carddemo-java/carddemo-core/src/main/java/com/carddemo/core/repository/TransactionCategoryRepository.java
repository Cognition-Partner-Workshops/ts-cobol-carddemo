package com.carddemo.core.repository;

import com.carddemo.core.domain.TransactionCategory;
import com.carddemo.core.domain.TransactionCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TransactionCategory entity.
 * Replaces VSAM operations on TRANCATG file and DB2 TRANSACTION_TYPE_CATEGORY table.
 */
@Repository
public interface TransactionCategoryRepository
        extends JpaRepository<TransactionCategory, TransactionCategoryId> {

    List<TransactionCategory> findByTypeCode(String typeCode);
}
