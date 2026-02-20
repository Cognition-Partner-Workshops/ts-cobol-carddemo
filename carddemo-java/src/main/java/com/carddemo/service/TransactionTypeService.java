package com.carddemo.service;

import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryId;
import com.carddemo.entity.TransactionType;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    public TransactionTypeService(TransactionTypeRepository transactionTypeRepository,
                                  TransactionCategoryRepository transactionCategoryRepository) {
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
    }

    public List<TransactionType> listTransactionTypes() {
        return transactionTypeRepository.findAll();
    }

    public TransactionType getTransactionType(String typeCd) {
        return transactionTypeRepository.findById(typeCd)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction type not found: " + typeCd));
    }

    @Transactional
    public TransactionType createTransactionType(TransactionType type) {
        return transactionTypeRepository.save(type);
    }

    @Transactional
    public TransactionType updateTransactionType(String typeCd, TransactionType updated) {
        TransactionType existing = transactionTypeRepository.findById(typeCd)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction type not found: " + typeCd));
        if (updated.getTypeDesc() != null) {
            existing.setTypeDesc(updated.getTypeDesc());
        }
        return transactionTypeRepository.save(existing);
    }

    @Transactional
    public void deleteTransactionType(String typeCd) {
        if (!transactionTypeRepository.existsById(typeCd)) {
            throw new ResourceNotFoundException("Transaction type not found: " + typeCd);
        }
        transactionTypeRepository.deleteById(typeCd);
    }

    public List<TransactionCategory> listCategoriesByType(String typeCd) {
        return transactionCategoryRepository.findByTypeCd(typeCd);
    }

    @Transactional
    public TransactionCategory createCategory(TransactionCategory category) {
        return transactionCategoryRepository.save(category);
    }

    @Transactional
    public TransactionCategory updateCategory(String typeCd, Integer catCd,
                                              TransactionCategory updated) {
        TransactionCategoryId id = new TransactionCategoryId(typeCd, catCd);
        TransactionCategory existing = transactionCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction category not found: " + typeCd + "/" + catCd));
        if (updated.getCatTypeDesc() != null) {
            existing.setCatTypeDesc(updated.getCatTypeDesc());
        }
        return transactionCategoryRepository.save(existing);
    }

    @Transactional
    public void deleteCategory(String typeCd, Integer catCd) {
        TransactionCategoryId id = new TransactionCategoryId(typeCd, catCd);
        if (!transactionCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Transaction category not found: " + typeCd + "/" + catCd);
        }
        transactionCategoryRepository.deleteById(id);
    }
}
