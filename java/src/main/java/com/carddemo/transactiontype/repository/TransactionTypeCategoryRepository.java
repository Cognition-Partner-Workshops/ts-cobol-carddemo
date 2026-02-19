package com.carddemo.transactiontype.repository;

import com.carddemo.transactiontype.model.TransactionTypeCategory;
import com.carddemo.transactiontype.model.TransactionTypeCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionTypeCategoryRepository
        extends JpaRepository<TransactionTypeCategory, TransactionTypeCategoryId> {

    List<TransactionTypeCategory> findByTrcTypeCode(String trcTypeCode);

    boolean existsByTrcTypeCode(String trcTypeCode);
}
