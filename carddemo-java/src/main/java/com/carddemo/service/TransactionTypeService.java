package com.carddemo.service;

import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryId;
import com.carddemo.entity.TransactionType;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TransactionTypeService {

    private final TransactionTypeRepository typeRepository;
    private final TransactionCategoryRepository categoryRepository;

    public TransactionTypeService(TransactionTypeRepository typeRepository,
                                  TransactionCategoryRepository categoryRepository) {
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionType> listTransactionTypes() {
        return typeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TransactionType getTransactionType(String typeCd) {
        return typeRepository.findById(typeCd)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type not found: " + typeCd));
    }

    public TransactionType addTransactionType(TransactionType type) {
        if (typeRepository.findById(type.getTypeCd()).isPresent()) {
            throw new ValidationException("Transaction type already exists: " + type.getTypeCd());
        }
        return typeRepository.save(type);
    }

    public TransactionType updateTransactionType(String typeCd, TransactionType type) {
        TransactionType existing = typeRepository.findById(typeCd)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type not found: " + typeCd));
        if (type.getTypeDesc() != null) {
            existing.setTypeDesc(type.getTypeDesc());
        }
        return typeRepository.save(existing);
    }

    public void deleteTransactionType(String typeCd) {
        TransactionType type = typeRepository.findById(typeCd)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type not found: " + typeCd));

        List<TransactionCategory> categories = categoryRepository.findByTypeCd(typeCd);
        if (!categories.isEmpty()) {
            throw new ValidationException(
                    "Cannot delete transaction type with existing categories. Delete categories first.");
        }

        typeRepository.delete(type);
    }

    @Transactional(readOnly = true)
    public List<TransactionCategory> listCategories(String typeCd) {
        return categoryRepository.findByTypeCd(typeCd);
    }

    public TransactionCategory addCategory(TransactionCategory category) {
        typeRepository.findById(category.getTypeCd())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction type not found: " + category.getTypeCd()));

        TransactionCategoryId id = new TransactionCategoryId(category.getTypeCd(), category.getCatCd());
        if (categoryRepository.findById(id).isPresent()) {
            throw new ValidationException("Transaction category already exists");
        }
        return categoryRepository.save(category);
    }

    public void deleteCategory(String typeCd, Integer catCd) {
        TransactionCategoryId id = new TransactionCategoryId(typeCd, catCd);
        TransactionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction category not found"));
        categoryRepository.delete(category);
    }
}
