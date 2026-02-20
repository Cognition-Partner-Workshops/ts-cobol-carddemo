package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Customer;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository,
                          CardCrossReferenceRepository cardCrossReferenceRepository,
                          CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.customerRepository = customerRepository;
    }

    public Map<String, Object> getAccountView(Long acctId) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Did not find this account in account master file"));

        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findByAcctId(acctId);
        Customer customer = null;
        if (!xrefs.isEmpty()) {
            customer = customerRepository.findById(xrefs.get(0).getCustId()).orElse(null);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("account", account);
        result.put("customer", customer);
        result.put("cardReferences", xrefs);
        return result;
    }

    public Page<Account> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    @Transactional
    public Account updateAccount(Long acctId, Account updatedAccount) {
        Account existing = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + acctId));

        if (updatedAccount.getActiveStatus() != null) {
            existing.setActiveStatus(updatedAccount.getActiveStatus());
        }
        if (updatedAccount.getCreditLimit() != null) {
            existing.setCreditLimit(updatedAccount.getCreditLimit());
        }
        if (updatedAccount.getCashCreditLimit() != null) {
            existing.setCashCreditLimit(updatedAccount.getCashCreditLimit());
        }
        if (updatedAccount.getExpirationDate() != null) {
            existing.setExpirationDate(updatedAccount.getExpirationDate());
        }
        if (updatedAccount.getReissueDate() != null) {
            existing.setReissueDate(updatedAccount.getReissueDate());
        }
        if (updatedAccount.getGroupId() != null) {
            existing.setGroupId(updatedAccount.getGroupId());
        }

        return accountRepository.save(existing);
    }

    public Account getAccount(Long acctId) {
        return accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + acctId));
    }
}
