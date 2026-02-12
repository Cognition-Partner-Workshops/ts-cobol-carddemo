package com.carddemo.service;

import com.carddemo.repository.AccountRepository;
import org.springframework.stereotype.Service;

/**
 * Business logic for account management.
 * Will contain migrated logic from:
 * - COACTVWC.cbl (Account View)
 * - COACTUPC.cbl (Account Update)
 * - CBACT01C.cbl (Account batch processing)
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
}
