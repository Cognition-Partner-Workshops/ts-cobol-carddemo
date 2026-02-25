package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Customer;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Account service - migrated from COACTVWC (CAVW Account View) and COACTUPC (CAUP Account Update).
 *
 * COACTVWC logic:
 * 1. Receive account ID from BMS map
 * 2. Read CXACAIX (card-account xref by account path) to find associated cards/customer
 * 3. Read ACCTDAT (account master) by account ID
 * 4. Read CUSTDAT (customer master) by customer ID from xref
 * 5. Display account details with customer info
 *
 * COACTUPC logic:
 * 1. Similar to view but allows editing account fields
 * 2. REWRITE account record to ACCTDAT
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository,
                          CardAccountXrefRepository xrefRepository,
                          CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.xrefRepository = xrefRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * View account details - migrated from COACTVWC 9000-READ-ACCT paragraph.
     */
    public Optional<Account> viewAccount(Long acctId) {
        return accountRepository.findById(acctId);
    }

    /**
     * Get customer associated with an account via the card-account xref.
     * Migrated from COACTVWC xref lookup logic.
     */
    public Optional<Customer> getCustomerForAccount(Long acctId) {
        List<CardAccountXref> xrefs = xrefRepository.findByAcctId(acctId);
        if (xrefs.isEmpty()) {
            return Optional.empty();
        }
        return customerRepository.findById(xrefs.get(0).getCustId());
    }

    /**
     * Update account - migrated from COACTUPC 9000-UPDATE-ACCT paragraph.
     * Replaces CICS REWRITE on ACCTDAT dataset.
     */
    @Transactional
    public Account updateAccount(Long acctId, Account updatedData) {
        Account existing = accountRepository.findById(acctId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Did not find this account in account master file"));

        if (updatedData.getActiveStatus() != null) {
            existing.setActiveStatus(updatedData.getActiveStatus());
        }
        if (updatedData.getCreditLimit() != null) {
            existing.setCreditLimit(updatedData.getCreditLimit());
        }
        if (updatedData.getCashCreditLimit() != null) {
            existing.setCashCreditLimit(updatedData.getCashCreditLimit());
        }
        if (updatedData.getExpirationDate() != null) {
            existing.setExpirationDate(updatedData.getExpirationDate());
        }
        if (updatedData.getReissueDate() != null) {
            existing.setReissueDate(updatedData.getReissueDate());
        }
        if (updatedData.getAddressZip() != null) {
            existing.setAddressZip(updatedData.getAddressZip());
        }
        if (updatedData.getGroupId() != null) {
            existing.setGroupId(updatedData.getGroupId());
        }

        return accountRepository.save(existing);
    }

    /**
     * Get all accounts.
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Apply a payment to reduce account balance.
     * Migrated from COBIL00C bill payment logic.
     */
    @Transactional
    public Account applyPayment(Long acctId, BigDecimal paymentAmount) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        BigDecimal newBalance = account.getCurrentBalance().subtract(paymentAmount);
        account.setCurrentBalance(newBalance);

        BigDecimal newCycleCredit = account.getCurrentCycleCredit() != null
                ? account.getCurrentCycleCredit().add(paymentAmount)
                : paymentAmount;
        account.setCurrentCycleCredit(newCycleCredit);

        return accountRepository.save(account);
    }
}
