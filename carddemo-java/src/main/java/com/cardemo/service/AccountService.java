package com.cardemo.service;

import com.cardemo.entity.Account;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Account management service.
 * Migrated from COACTVWC (CAVW - view) and COACTUPC (CAUP - update).
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * View account - migrated from COACTVWC (CAVW transaction).
     * COBOL: EXEC CICS READ DATASET(WS-ACCTFILENAME) INTO(ACCOUNT-RECORD)
     */
    public Account getAccount(Long acctId) {
        return accountRepository.findById(acctId)
                .orElseThrow(() -> CardDemoException.notFound("Account not found: " + acctId));
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Update account - migrated from COACTUPC (CAUP transaction).
     * COBOL: EXEC CICS REWRITE DATASET(WS-ACCTFILENAME) FROM(ACCOUNT-RECORD)
     */
    @Transactional
    public Account updateAccount(Long acctId, Account updatedAccount) {
        Account existing = accountRepository.findById(acctId)
                .orElseThrow(() -> CardDemoException.notFound("Account not found: " + acctId));

        if (updatedAccount.getAcctActiveStatus() != null) {
            existing.setAcctActiveStatus(updatedAccount.getAcctActiveStatus());
        }
        if (updatedAccount.getAcctCreditLimit() != null) {
            existing.setAcctCreditLimit(updatedAccount.getAcctCreditLimit());
        }
        if (updatedAccount.getAcctCashCreditLimit() != null) {
            existing.setAcctCashCreditLimit(updatedAccount.getAcctCashCreditLimit());
        }
        if (updatedAccount.getAcctExpirationDate() != null) {
            existing.setAcctExpirationDate(updatedAccount.getAcctExpirationDate());
        }
        if (updatedAccount.getAcctReissueDate() != null) {
            existing.setAcctReissueDate(updatedAccount.getAcctReissueDate());
        }
        if (updatedAccount.getAcctAddrZip() != null) {
            existing.setAcctAddrZip(updatedAccount.getAcctAddrZip());
        }
        if (updatedAccount.getAcctGroupId() != null) {
            existing.setAcctGroupId(updatedAccount.getAcctGroupId());
        }

        return accountRepository.save(existing);
    }
}
