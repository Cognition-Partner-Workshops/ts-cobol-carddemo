package com.carddemo.transaction.repository;

import com.carddemo.common.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategory.TransactionCategoryId> {

    List<TransactionCategory> findByTransactionTypeCode(String transactionTypeCode);
}
