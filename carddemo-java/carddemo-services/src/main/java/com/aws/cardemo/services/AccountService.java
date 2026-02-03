package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Account;
import com.aws.cardemo.persistence.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Optional<Account> getAccountById(String accountId) {
        return accountRepository.findById(accountId);
    }

    public Optional<Account> getActiveAccountById(String accountId) {
        return accountRepository.findActiveAccountById(accountId);
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }

    public void deleteAccount(String accountId) {
        accountRepository.deleteById(accountId);
    }

    public List<Account> getAccountsByStatus(String status) {
        return accountRepository.findByAccountStatus(status);
    }

    public List<Account> getAccountsByGroupId(String groupId) {
        return accountRepository.findByGroupId(groupId);
    }

    public List<Account> getAccountsOverCreditLimit() {
        return accountRepository.findAccountsOverCreditLimit();
    }

    public Account updateBalance(String accountId, BigDecimal amount, boolean isCredit) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        BigDecimal currentBalance = account.getCurrentBalance();
        if (isCredit) {
            account.setCurrentBalance(currentBalance.subtract(amount));
            account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(amount));
        } else {
            account.setCurrentBalance(currentBalance.add(amount));
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(amount));
        }

        return accountRepository.save(account);
    }

    public boolean isAccountOverCreditLimit(String accountId) {
        return accountRepository.findById(accountId)
                .map(account -> account.getCurrentBalance().compareTo(account.getCreditLimit()) > 0)
                .orElse(false);
    }
}
