package com.carddemo.service;

import com.carddemo.repository.TransactionRepository;
import org.springframework.stereotype.Service;

/**
 * Business logic for transaction processing.
 * Will contain migrated logic from:
 * - COTRN00C.cbl (Transaction List)
 * - COTRN01C.cbl (Transaction View)
 * - COTRN02C.cbl (Transaction Add)
 * - CBTRN01C.cbl (Transaction batch processing)
 * - CBTRN02C.cbl (Interest calculation batch)
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
}
