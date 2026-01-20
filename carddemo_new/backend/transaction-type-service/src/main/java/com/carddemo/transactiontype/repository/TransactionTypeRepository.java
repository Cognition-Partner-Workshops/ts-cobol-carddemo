package com.carddemo.transactiontype.repository;

import com.carddemo.transactiontype.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {
    List<TransactionType> findByActiveTrue();
    
    List<TransactionType> findByCategoryCode(String categoryCode);
    
    List<TransactionType> findByDebitCreditIndicator(String indicator);
    
    @Query("SELECT t FROM TransactionType t WHERE t.active = true ORDER BY t.displayOrder ASC")
    List<TransactionType> findActiveTypesOrdered();
}
