package com.carddemo.batch.service;

import com.carddemo.batch.dto.TransactionPostingResult;
import com.carddemo.batch.entity.Account;
import com.carddemo.batch.entity.Transaction;
import com.carddemo.batch.repository.AccountRepository;
import com.carddemo.batch.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionPostingService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_FAILED = "FAILED";
    
    // Transaction type codes
    private static final String TYPE_PURCHASE = "PR";
    private static final String TYPE_PAYMENT = "PA";
    private static final String TYPE_CASH_ADVANCE = "CA";
    private static final String TYPE_REFUND = "RF";
    private static final String TYPE_FEE = "FE";
    private static final String TYPE_INTEREST = "IN";
    
    @Transactional
    public List<TransactionPostingResult> postPendingTransactions() {
        log.info("Starting transaction posting batch");
        List<TransactionPostingResult> results = new ArrayList<>();
        
        List<Transaction> pendingTransactions = transactionRepository.findPendingTransactions();
        log.info("Found {} pending transactions to post", pendingTransactions.size());
        
        for (Transaction transaction : pendingTransactions) {
            try {
                TransactionPostingResult result = postTransaction(transaction);
                results.add(result);
            } catch (Exception e) {
                log.error("Error posting transaction {}: {}", transaction.getTransactionId(), e.getMessage());
                results.add(TransactionPostingResult.builder()
                    .transactionId(transaction.getTransactionId())
                    .accountId(transaction.getAccountId())
                    .amount(transaction.getAmount())
                    .previousStatus(transaction.getStatus())
                    .newStatus(STATUS_FAILED)
                    .errorMessage(e.getMessage())
                    .build());
            }
        }
        
        log.info("Completed transaction posting. Processed {} transactions", results.size());
        return results;
    }
    
    @Transactional
    public TransactionPostingResult postTransaction(Transaction transaction) {
        String previousStatus = transaction.getStatus();
        
        // Validate account exists
        Optional<Account> accountOpt = accountRepository.findById(transaction.getAccountId());
        if (accountOpt.isEmpty()) {
            transaction.setStatus(STATUS_FAILED);
            transactionRepository.save(transaction);
            return TransactionPostingResult.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(transaction.getAccountId())
                .amount(transaction.getAmount())
                .previousStatus(previousStatus)
                .newStatus(STATUS_FAILED)
                .errorMessage("Account not found")
                .build();
        }
        
        Account account = accountOpt.get();
        
        // Validate account is active
        if (!"Y".equals(account.getActiveStatus())) {
            transaction.setStatus(STATUS_FAILED);
            transactionRepository.save(transaction);
            return TransactionPostingResult.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(transaction.getAccountId())
                .amount(transaction.getAmount())
                .previousStatus(previousStatus)
                .newStatus(STATUS_FAILED)
                .errorMessage("Account is not active")
                .build();
        }
        
        // Update account balance based on transaction type
        BigDecimal amount = transaction.getAmount();
        BigDecimal currentBalance = account.getCurrentBalance();
        BigDecimal newBalance;
        
        String typeCode = transaction.getTypeCode();
        if (TYPE_PAYMENT.equals(typeCode) || TYPE_REFUND.equals(typeCode)) {
            // Credits reduce balance
            newBalance = currentBalance.subtract(amount);
            account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(amount));
        } else {
            // Debits increase balance (purchases, cash advances, fees, interest)
            newBalance = currentBalance.add(amount);
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(amount));
        }
        
        // Validate credit limit for debits
        if (newBalance.compareTo(account.getCreditLimit()) > 0 && 
            !TYPE_PAYMENT.equals(typeCode) && !TYPE_REFUND.equals(typeCode)) {
            transaction.setStatus(STATUS_FAILED);
            transactionRepository.save(transaction);
            return TransactionPostingResult.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(transaction.getAccountId())
                .amount(amount)
                .previousStatus(previousStatus)
                .newStatus(STATUS_FAILED)
                .errorMessage("Transaction would exceed credit limit")
                .build();
        }
        
        // Update account balance
        account.setCurrentBalance(newBalance);
        accountRepository.save(account);
        
        // Update transaction status
        LocalDateTime postedTime = LocalDateTime.now();
        transaction.setStatus(STATUS_POSTED);
        transaction.setPostedTimestamp(postedTime);
        transactionRepository.save(transaction);
        
        log.info("Posted transaction {} for account {}: {} {}", 
            transaction.getTransactionId(), account.getAccountId(), typeCode, amount);
        
        return TransactionPostingResult.builder()
            .transactionId(transaction.getTransactionId())
            .accountId(transaction.getAccountId())
            .amount(amount)
            .previousStatus(previousStatus)
            .newStatus(STATUS_POSTED)
            .postedTimestamp(postedTime)
            .build();
    }
}
