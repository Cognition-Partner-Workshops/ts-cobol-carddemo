package com.aws.carddemo.transaction;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.transaction.dto.TransactionTypeResponse;

@Service
@Transactional(readOnly = true)
public class TransactionTypeService {

    private final TransactionTypeRepository transactionTypeRepository;

    public TransactionTypeService(TransactionTypeRepository transactionTypeRepository) {
        this.transactionTypeRepository = transactionTypeRepository;
    }

    public List<TransactionTypeResponse> listAll() {
        return transactionTypeRepository.findAll().stream()
                .map(TransactionTypeResponse::from)
                .toList();
    }

    public TransactionTypeResponse getByCode(String typeCode) {
        TransactionType type = transactionTypeRepository.findById(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction type not found with code: " + typeCode));
        return TransactionTypeResponse.from(type);
    }
}
