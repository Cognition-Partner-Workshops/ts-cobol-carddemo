package com.carddemo.api.service;

import com.carddemo.api.dto.TransactionTypeRequest;
import com.carddemo.api.dto.TransactionTypeResponse;
import com.carddemo.core.domain.TransactionType;
import com.carddemo.core.exception.DuplicateResourceException;
import com.carddemo.core.exception.ResourceNotFoundException;
import com.carddemo.core.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Transaction Type management.
 * Replaces business logic from COTRTUPC (Transaction Type Update)
 * and COTRTLIC (Transaction Type List) in the DB2 module.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;

    public List<TransactionTypeResponse> listTransactionTypes() {
        return transactionTypeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TransactionTypeResponse createTransactionType(TransactionTypeRequest request) {
        if (transactionTypeRepository.existsById(request.getTypeCode())) {
            throw new DuplicateResourceException("TransactionType", request.getTypeCode());
        }

        TransactionType type = TransactionType.builder()
                .typeCode(request.getTypeCode())
                .description(request.getDescription())
                .build();

        TransactionType saved = transactionTypeRepository.save(type);
        return mapToResponse(saved);
    }

    @Transactional
    public TransactionTypeResponse updateTransactionType(String typeCode, TransactionTypeRequest request) {
        TransactionType type = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException("TransactionType", typeCode));

        type.setDescription(request.getDescription());

        TransactionType saved = transactionTypeRepository.save(type);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteTransactionType(String typeCode) {
        TransactionType type = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException("TransactionType", typeCode));
        transactionTypeRepository.delete(type);
    }

    private TransactionTypeResponse mapToResponse(TransactionType type) {
        return TransactionTypeResponse.builder()
                .typeCode(type.getTypeCode())
                .description(type.getDescription())
                .build();
    }
}
