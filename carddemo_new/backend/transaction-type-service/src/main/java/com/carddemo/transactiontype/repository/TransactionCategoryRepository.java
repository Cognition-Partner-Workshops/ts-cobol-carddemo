package com.carddemo.transactiontype.repository;

import com.carddemo.transactiontype.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, String> {
    List<TransactionCategory> findByActiveTrue();
    
    List<TransactionCategory> findByParentCategoryCode(String parentCode);
    
    List<TransactionCategory> findByCategoryType(String categoryType);
    
    List<TransactionCategory> findByReportingGroup(String reportingGroup);
    
    @Query("SELECT c FROM TransactionCategory c WHERE c.active = true ORDER BY c.displayOrder ASC")
    List<TransactionCategory> findActiveCategoriesOrdered();
    
    @Query("SELECT c FROM TransactionCategory c WHERE c.parentCategoryCode IS NULL AND c.active = true ORDER BY c.displayOrder ASC")
    List<TransactionCategory> findRootCategories();
}
