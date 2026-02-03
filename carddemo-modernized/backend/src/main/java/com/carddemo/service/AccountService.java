package com.carddemo.service;

import com.carddemo.exception.BadRequestException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.model.Account;
import com.carddemo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final AccountRepository accountRepository;
    
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }
    
    public Account getAccountById(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", accountId));
    }
    
    public Account createAccount(Account account) {
        if (accountRepository.existsByAccountId(account.getAccountId())) {
            throw new BadRequestException("Account with ID " + account.getAccountId() + " already exists");
        }
        return accountRepository.save(account);
    }
    
    public Account updateAccount(String accountId, Account accountDetails) {
        Account account = getAccountById(accountId);
        
        account.setActiveStatus(accountDetails.getActiveStatus());
        account.setCurrentBalance(accountDetails.getCurrentBalance());
        account.setCreditLimit(accountDetails.getCreditLimit());
        account.setCashCreditLimit(accountDetails.getCashCreditLimit());
        account.setOpenDate(accountDetails.getOpenDate());
        account.setExpirationDate(accountDetails.getExpirationDate());
        account.setReissueDate(accountDetails.getReissueDate());
        account.setCurrentCycleCredit(accountDetails.getCurrentCycleCredit());
        account.setCurrentCycleDebit(accountDetails.getCurrentCycleDebit());
        account.setZipCode(accountDetails.getZipCode());
        account.setGroupId(accountDetails.getGroupId());
        
        return accountRepository.save(account);
    }
    
    public void deleteAccount(String accountId) {
        if (!accountRepository.existsByAccountId(accountId)) {
            throw new ResourceNotFoundException("Account", "accountId", accountId);
        }
        accountRepository.deleteByAccountId(accountId);
    }
}
