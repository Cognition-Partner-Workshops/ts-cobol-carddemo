package com.aws.carddemo.transaction;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.transaction.dto.TransactionCategoryResponse;

@Service
@Transactional(readOnly = true)
public class TransactionCategoryService {

    private final TransactionCategoryRepository transactionCategoryRepository;

    public TransactionCategoryService(TransactionCategoryRepository transactionCategoryRepository) {
        this.transactionCategoryRepository = transactionCategoryRepository;
    }

    public List<TransactionCategoryResponse> listAll() {
        return transactionCategoryRepository.findAll().stream()
                .map(TransactionCategoryResponse::from)
                .toList();
    }

    public TransactionCategoryResponse getByCode(String catCode) {
        TransactionCategory category = transactionCategoryRepository.findById(catCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction category not found with code: " + catCode));
        return TransactionCategoryResponse.from(category);
    }
}
