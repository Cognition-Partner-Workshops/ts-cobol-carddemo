package com.cardemo.repository;

import com.cardemo.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategory.TransactionCategoryId> {
}
