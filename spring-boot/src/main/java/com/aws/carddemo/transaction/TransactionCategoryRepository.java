package com.aws.carddemo.transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, String> {

    List<TransactionCategory> findByTransactionTypeTypeCd(String typeCd);
}
