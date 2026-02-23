package com.carddemo.core.repository;

import com.carddemo.core.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for TransactionType entity.
 * Replaces VSAM operations on TRANTYPE file and DB2 TRANSACTION_TYPE table.
 */
@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {
}
