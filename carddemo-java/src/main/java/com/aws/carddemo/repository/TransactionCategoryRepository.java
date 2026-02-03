package com.aws.carddemo.repository;

import com.aws.carddemo.model.TransactionCategory;
import com.aws.carddemo.model.TransactionCategory.TransactionCategoryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategoryKey> {

    List<TransactionCategory> findByIdTranTypeCd(String tranTypeCd);
}
