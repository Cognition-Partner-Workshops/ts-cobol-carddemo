package com.carddemo.batch.service;

import com.carddemo.batch.dto.InterestCalculationResult;
import com.carddemo.batch.entity.Account;
import com.carddemo.batch.entity.Transaction;
import com.carddemo.batch.repository.AccountRepository;
import com.carddemo.batch.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestCalculationService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final String INTEREST_TYPE_CODE = "IN";
    private static final String INTEREST_CATEGORY = "5000";
    
    @Transactional
    public List<InterestCalculationResult> calculateDailyInterest() {
        log.info("Starting daily interest calculation");
        List<InterestCalculationResult> results = new ArrayList<>();
        
        List<Account> accountsWithBalance = accountRepository.findAccountsWithBalance();
        log.info("Found {} accounts with balance for interest calculation", accountsWithBalance.size());
        
        for (Account account : accountsWithBalance) {
            try {
                InterestCalculationResult result = calculateInterestForAccount(account);
                results.add(result);
            } catch (Exception e) {
                log.error("Error calculating interest for account {}: {}", account.getAccountId(), e.getMessage());
                results.add(InterestCalculationResult.builder()
                    .accountId(account.getAccountId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());
            }
        }
        
        log.info("Completed daily interest calculation. Processed {} accounts", results.size());
        return results;
    }
    
    @Transactional
    public InterestCalculationResult calculateInterestForAccount(Account account) {
        BigDecimal balance = account.getCurrentBalance();
        BigDecimal annualRate = account.getInterestRate();
        
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            return InterestCalculationResult.builder()
                .accountId(account.getAccountId())
                .previousBalance(balance)
                .interestRate(BigDecimal.ZERO)
                .interestAmount(BigDecimal.ZERO)
                .newBalance(balance)
                .status("SKIPPED")
                .errorMessage("No interest rate configured")
                .build();
        }
        
        // Calculate daily interest: balance * (annual_rate / 365) / 100
        BigDecimal dailyRate = annualRate.divide(DAYS_IN_YEAR, 10, RoundingMode.HALF_UP)
            .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal interestAmount = balance.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
        
        // Create interest transaction
        Transaction interestTransaction = new Transaction();
        interestTransaction.setTransactionId(generateTransactionId());
        interestTransaction.setTypeCode(INTEREST_TYPE_CODE);
        interestTransaction.setCategoryCode(INTEREST_CATEGORY);
        interestTransaction.setSource("BATCH");
        interestTransaction.setDescription("Daily Interest Charge");
        interestTransaction.setAmount(interestAmount);
        interestTransaction.setAccountId(account.getAccountId());
        interestTransaction.setOriginalTimestamp(LocalDateTime.now());
        interestTransaction.setPostedTimestamp(LocalDateTime.now());
        interestTransaction.setStatus("POSTED");
        interestTransaction.setInterestAmount(interestAmount);
        
        transactionRepository.save(interestTransaction);
        
        // Update account balance
        BigDecimal newBalance = balance.add(interestAmount);
        account.setCurrentBalance(newBalance);
        account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(interestAmount));
        accountRepository.save(account);
        
        log.info("Calculated interest for account {}: {} at rate {}%", 
            account.getAccountId(), interestAmount, annualRate);
        
        return InterestCalculationResult.builder()
            .accountId(account.getAccountId())
            .previousBalance(balance)
            .interestRate(annualRate)
            .interestAmount(interestAmount)
            .newBalance(newBalance)
            .status("SUCCESS")
            .build();
    }
    
    private String generateTransactionId() {
        return "INT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
