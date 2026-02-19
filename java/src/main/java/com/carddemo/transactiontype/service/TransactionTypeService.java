package com.carddemo.transactiontype.service;

import com.carddemo.transactiontype.exception.TransactionTypeConflictException;
import com.carddemo.transactiontype.exception.TransactionTypeLockException;
import com.carddemo.transactiontype.exception.TransactionTypeNotFoundException;
import com.carddemo.transactiontype.model.TransactionType;
import com.carddemo.transactiontype.repository.TransactionTypeCategoryRepository;
import com.carddemo.transactiontype.repository.TransactionTypeRepository;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionTypeCategoryRepository categoryRepository;

    public TransactionTypeService(TransactionTypeRepository transactionTypeRepository,
                                  TransactionTypeCategoryRepository categoryRepository) {
        this.transactionTypeRepository = transactionTypeRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionType> findAll() {
        return transactionTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TransactionType findByTypeCode(String typeCode) {
        return transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new TransactionTypeNotFoundException(typeCode));
    }

    public TransactionType create(TransactionType transactionType) {
        if (transactionTypeRepository.existsById(transactionType.getTrType())) {
            throw new TransactionTypeConflictException(
                    "Transaction type already exists with code: " + transactionType.getTrType());
        }
        return transactionTypeRepository.save(transactionType);
    }

    public TransactionType update(String typeCode, TransactionType updated) {
        TransactionType existing = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new TransactionTypeNotFoundException(typeCode));

        try {
            existing.setTrDescription(updated.getTrDescription());
            return transactionTypeRepository.save(existing);
        } catch (CannotAcquireLockException e) {
            throw new TransactionTypeLockException(typeCode);
        }
    }

    public void delete(String typeCode) {
        if (!transactionTypeRepository.existsById(typeCode)) {
            throw new TransactionTypeNotFoundException(typeCode);
        }

        if (categoryRepository.existsByTrcTypeCode(typeCode)) {
            throw new TransactionTypeConflictException(
                    "Please delete associated child records first. "
                            + "Transaction type code '" + typeCode
                            + "' has dependent category records.");
        }

        try {
            transactionTypeRepository.deleteById(typeCode);
        } catch (DataIntegrityViolationException e) {
            throw new TransactionTypeConflictException(
                    "Please delete associated child records first. "
                            + "Transaction type code '" + typeCode
                            + "' has dependent category records.");
        }
    }
}
