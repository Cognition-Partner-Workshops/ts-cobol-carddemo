package com.cardemo.repository;

import com.cardemo.entity.TransactionTypeCategoryDb2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionTypeCategoryDb2Repository extends JpaRepository<TransactionTypeCategoryDb2, TransactionTypeCategoryDb2.TransactionTypeCategoryId> {
    List<TransactionTypeCategoryDb2> findByTrcTypeCode(String trcTypeCode);
}
