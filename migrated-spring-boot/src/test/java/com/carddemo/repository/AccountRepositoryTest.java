package com.carddemo.repository;

import com.carddemo.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAccountId(12345678901L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrentBalance(new BigDecimal("1500.00"));
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCashCreditLimit(new BigDecimal("1000.00"));
        testAccount.setOpenDate(LocalDate.of(2020, 1, 15));
        testAccount.setExpirationDate(LocalDate.of(2025, 1, 15));
        testAccount.setCurrentCycleCredit(BigDecimal.ZERO);
        testAccount.setCurrentCycleDebit(BigDecimal.ZERO);
        testAccount.setAddressZip("12345");
        testAccount.setGroupId("GRP001");
    }

    @Test
    void testSaveAndFindById() {
        Account saved = accountRepository.save(testAccount);
        
        Optional<Account> found = accountRepository.findById(saved.getAccountId());
        
        assertTrue(found.isPresent());
        assertEquals(testAccount.getAccountId(), found.get().getAccountId());
        assertEquals(testAccount.getActiveStatus(), found.get().getActiveStatus());
        assertEquals(0, testAccount.getCurrentBalance().compareTo(found.get().getCurrentBalance()));
    }

    @Test
    void testFindByActiveStatus() {
        accountRepository.save(testAccount);
        
        Account inactiveAccount = new Account();
        inactiveAccount.setAccountId(98765432101L);
        inactiveAccount.setActiveStatus("N");
        inactiveAccount.setCurrentBalance(new BigDecimal("500.00"));
        inactiveAccount.setCreditLimit(new BigDecimal("3000.00"));
        inactiveAccount.setCashCreditLimit(new BigDecimal("500.00"));
        inactiveAccount.setCurrentCycleCredit(BigDecimal.ZERO);
        inactiveAccount.setCurrentCycleDebit(BigDecimal.ZERO);
        accountRepository.save(inactiveAccount);
        
        List<Account> activeAccounts = accountRepository.findByActiveStatus("Y");
        List<Account> inactiveAccounts = accountRepository.findByActiveStatus("N");
        
        assertEquals(1, activeAccounts.size());
        assertEquals(1, inactiveAccounts.size());
        assertEquals(testAccount.getAccountId(), activeAccounts.get(0).getAccountId());
    }

    @Test
    void testFindByActiveStatusWithPagination() {
        accountRepository.save(testAccount);
        
        Page<Account> page = accountRepository.findByActiveStatus("Y", PageRequest.of(0, 10));
        
        assertEquals(1, page.getTotalElements());
        assertEquals(testAccount.getAccountId(), page.getContent().get(0).getAccountId());
    }

    @Test
    void testFindByGroupId() {
        accountRepository.save(testAccount);
        
        List<Account> accounts = accountRepository.findByGroupId("GRP001");
        
        assertEquals(1, accounts.size());
        assertEquals(testAccount.getAccountId(), accounts.get(0).getAccountId());
    }

    @Test
    void testFindByAddressZip() {
        accountRepository.save(testAccount);
        
        List<Account> accounts = accountRepository.findByAddressZip("12345");
        
        assertEquals(1, accounts.size());
    }

    @Test
    void testCountByActiveStatus() {
        accountRepository.save(testAccount);
        
        long count = accountRepository.countByActiveStatus("Y");
        
        assertEquals(1, count);
    }

    @Test
    void testFindAccountsOverCreditLimit() {
        testAccount.setCurrentBalance(new BigDecimal("6000.00"));
        accountRepository.save(testAccount);
        
        List<Account> overLimitAccounts = accountRepository.findAccountsOverCreditLimit();
        
        assertEquals(1, overLimitAccounts.size());
    }

    @Test
    void testFindByCurrentBalanceGreaterThan() {
        accountRepository.save(testAccount);
        
        List<Account> accounts = accountRepository.findByCurrentBalanceGreaterThan(new BigDecimal("1000.00"));
        
        assertEquals(1, accounts.size());
    }

    @Test
    void testDelete() {
        Account saved = accountRepository.save(testAccount);
        
        accountRepository.delete(saved);
        
        Optional<Account> found = accountRepository.findById(saved.getAccountId());
        assertFalse(found.isPresent());
    }
}
