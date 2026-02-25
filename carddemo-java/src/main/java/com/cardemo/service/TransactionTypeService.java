package com.cardemo.service;

import com.cardemo.entity.TransactionTypeDb2;
import com.cardemo.entity.TransactionTypeCategoryDb2;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.TransactionTypeDb2Repository;
import com.cardemo.repository.TransactionTypeCategoryDb2Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transaction type management service (Optional DB2 module).
 * Migrated from COTRTUPC (CTTU - add/update) and COTRTLIC (CTLI - list).
 */
@Service
public class TransactionTypeService {

    private final TransactionTypeDb2Repository typeRepository;
    private final TransactionTypeCategoryDb2Repository categoryRepository;

    public TransactionTypeService(TransactionTypeDb2Repository typeRepository,
                                  TransactionTypeCategoryDb2Repository categoryRepository) {
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * List transaction types with cursor-based paging - migrated from COTRTLIC (CTLI).
     * COBOL: EXEC SQL DECLARE C1 CURSOR FOR SELECT ... ORDER BY TR_TYPE
     */
    public Page<TransactionTypeDb2> listTransactionTypes(Pageable pageable) {
        return typeRepository.findAll(pageable);
    }

    /**
     * Get transaction type by code.
     */
    public TransactionTypeDb2 getTransactionType(String trType) {
        return typeRepository.findById(trType)
                .orElseThrow(() -> CardDemoException.notFound("Transaction type not found: " + trType));
    }

    /**
     * Create transaction type - migrated from COTRTUPC (CTTU transaction).
     * COBOL: EXEC SQL INSERT INTO TRANSACTION_TYPE VALUES (:TR-TYPE, :TR-DESCRIPTION)
     */
    @Transactional
    public TransactionTypeDb2 createTransactionType(TransactionTypeDb2 type) {
        if (typeRepository.existsById(type.getTrType())) {
            throw CardDemoException.conflict("Transaction type already exists: " + type.getTrType());
        }
        return typeRepository.save(type);
    }

    /**
     * Update transaction type - migrated from COTRTUPC (CTTU transaction).
     * COBOL: EXEC SQL UPDATE TRANSACTION_TYPE SET TR_DESCRIPTION = :TR-DESCRIPTION WHERE TR_TYPE = :TR-TYPE
     */
    @Transactional
    public TransactionTypeDb2 updateTransactionType(String trType, TransactionTypeDb2 type) {
        TransactionTypeDb2 existing = typeRepository.findById(trType)
                .orElseThrow(() -> CardDemoException.notFound("Transaction type not found: " + trType));

        if (type.getTrDescription() != null) {
            existing.setTrDescription(type.getTrDescription());
        }
        return typeRepository.save(existing);
    }

    /**
     * Get categories for a transaction type.
     */
    public List<TransactionTypeCategoryDb2> getCategoriesByType(String trcTypeCode) {
        return categoryRepository.findByTrcTypeCode(trcTypeCode);
    }
}
