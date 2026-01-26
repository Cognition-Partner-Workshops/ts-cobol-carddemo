package com.aws.carddemo.repository;

import com.aws.carddemo.entity.TransactionTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionTypeCategoryRepository extends JpaRepository<TransactionTypeCategory, Integer> {

    Optional<TransactionTypeCategory> findByCategoryCode(String categoryCode);

    List<TransactionTypeCategory> findByActive(Boolean active);

    @Query("SELECT c FROM TransactionTypeCategory c WHERE c.active = true ORDER BY c.categoryCode")
    List<TransactionTypeCategory> findAllActiveCategories();

    @Query("SELECT c FROM TransactionTypeCategory c JOIN FETCH c.transactionTypes WHERE c.categoryId = :categoryId")
    Optional<TransactionTypeCategory> findByIdWithTypes(Integer categoryId);

    boolean existsByCategoryCode(String categoryCode);
}
