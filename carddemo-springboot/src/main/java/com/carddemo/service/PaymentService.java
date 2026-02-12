package com.carddemo.service;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import org.springframework.stereotype.Service;

/**
 * Business logic for bill payment processing.
 * Will contain migrated logic from:
 * - COBIL00C.cbl (Bill Payment)
 */
@Service
public class PaymentService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public PaymentService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }
}
