package com.carddemo.service;

import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryId;
import com.carddemo.entity.TransactionType;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Transaction Type service - migrated from Phase 5a DB2 Transaction Type module:
 *   COTRTUPC (CTTU - Transaction Type Add/Edit)
 *   COTRTLIC (CTLI - Transaction Type List with forward/backward paging)
 *   COBTUPDT (batch - Maintain Transaction Type)
 *
 * COTRTUPC: INSERT/UPDATE on CARDDEMO.TRANSACTION_TYPE and TRANSACTION_TYPE_CATEGORY via DB2 cursors.
 * COTRTLIC: DECLARE CURSOR with ORDER BY for paginated list, supports NEXT/PREV navigation.
 */
@Service
public class TransactionTypeService {

    private final TransactionTypeRepository typeRepository;
    private final TransactionCategoryRepository categoryRepository;

    public TransactionTypeService(TransactionTypeRepository typeRepository,
                                  TransactionCategoryRepository categoryRepository) {
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<TransactionType> listTypes(Pageable pageable) {
        return typeRepository.findAll(pageable);
    }

    public Optional<TransactionType> getType(String typeCd) {
        return typeRepository.findById(typeCd);
    }

    @Transactional
    public TransactionType addType(TransactionType type) {
        if (typeRepository.existsById(type.getTypeCd())) {
            throw new IllegalArgumentException("Transaction type code already exists");
        }
        return typeRepository.save(type);
    }

    @Transactional
    public TransactionType updateType(String typeCd, TransactionType updatedData) {
        TransactionType existing = typeRepository.findById(typeCd)
                .orElseThrow(() -> new IllegalArgumentException("Transaction type not found"));
        if (updatedData.getTypeDesc() != null) {
            existing.setTypeDesc(updatedData.getTypeDesc());
        }
        return typeRepository.save(existing);
    }

    @Transactional
    public void deleteType(String typeCd) {
        // Check for dependent categories (FK constraint from TRANSACTION_TYPE_CATEGORY)
        List<TransactionCategory> categories = categoryRepository.findByTypeCd(typeCd);
        if (!categories.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete: transaction type has associated categories (DELETE RESTRICT)");
        }
        typeRepository.deleteById(typeCd);
    }

    public Page<TransactionCategory> listCategories(String typeCd, Pageable pageable) {
        return categoryRepository.findByTypeCd(typeCd, pageable);
    }

    public Optional<TransactionCategory> getCategory(String typeCd, Integer catCd) {
        return categoryRepository.findById(new TransactionCategoryId(typeCd, catCd));
    }

    @Transactional
    public TransactionCategory addCategory(TransactionCategory category) {
        TransactionCategoryId id = new TransactionCategoryId(category.getTypeCd(), category.getCatCd());
        if (categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Transaction category already exists");
        }
        if (!typeRepository.existsById(category.getTypeCd())) {
            throw new IllegalArgumentException("Parent transaction type does not exist");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public TransactionCategory updateCategory(String typeCd, Integer catCd, TransactionCategory updatedData) {
        TransactionCategoryId id = new TransactionCategoryId(typeCd, catCd);
        TransactionCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction category not found"));
        if (updatedData.getCatTypeDesc() != null) {
            existing.setCatTypeDesc(updatedData.getCatTypeDesc());
        }
        return categoryRepository.save(existing);
    }

    @Transactional
    public void deleteCategory(String typeCd, Integer catCd) {
        categoryRepository.deleteById(new TransactionCategoryId(typeCd, catCd));
    }
}
