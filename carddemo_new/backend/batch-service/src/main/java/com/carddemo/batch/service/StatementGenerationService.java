package com.carddemo.batch.service;

import com.carddemo.batch.dto.StatementGenerationResult;
import com.carddemo.batch.entity.Account;
import com.carddemo.batch.entity.Statement;
import com.carddemo.batch.entity.Transaction;
import com.carddemo.batch.repository.AccountRepository;
import com.carddemo.batch.repository.StatementRepository;
import com.carddemo.batch.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementGenerationService {
    
    private final AccountRepository accountRepository;
    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    
    private static final BigDecimal MINIMUM_PAYMENT_PERCENTAGE = new BigDecimal("0.02"); // 2% of balance
    private static final BigDecimal MINIMUM_PAYMENT_FLOOR = new BigDecimal("25.00");
    private static final int PAYMENT_DUE_DAYS = 25;
    
    // Transaction type codes
    private static final String TYPE_PURCHASE = "PR";
    private static final String TYPE_PAYMENT = "PA";
    private static final String TYPE_CASH_ADVANCE = "CA";
    private static final String TYPE_FEE = "FE";
    private static final String TYPE_INTEREST = "IN";
    
    @Transactional
    public List<StatementGenerationResult> generateMonthlyStatements() {
        log.info("Starting monthly statement generation");
        List<StatementGenerationResult> results = new ArrayList<>();
        
        List<Account> activeAccounts = accountRepository.findActiveAccounts();
        log.info("Found {} active accounts for statement generation", activeAccounts.size());
        
        LocalDate statementDate = LocalDate.now();
        
        for (Account account : activeAccounts) {
            try {
                StatementGenerationResult result = generateStatementForAccount(account, statementDate);
                results.add(result);
            } catch (Exception e) {
                log.error("Error generating statement for account {}: {}", account.getAccountId(), e.getMessage());
                results.add(StatementGenerationResult.builder()
                    .accountId(account.getAccountId())
                    .statementDate(statementDate)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());
            }
        }
        
        log.info("Completed statement generation. Generated {} statements", results.size());
        return results;
    }
    
    @Transactional
    public StatementGenerationResult generateStatementForAccount(Account account, LocalDate statementDate) {
        // Calculate statement period
        LocalDate periodEnd = statementDate;
        LocalDate periodStart = account.getLastStatementDate() != null 
            ? account.getLastStatementDate().plusDays(1) 
            : account.getOpenDate();
        
        // Get transactions for the period
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(23, 59, 59);
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
            account.getAccountId(), startDateTime, endDateTime);
        
        // Calculate statement totals
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        BigDecimal totalCashAdvances = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        
        for (Transaction txn : transactions) {
            if ("POSTED".equals(txn.getStatus())) {
                String typeCode = txn.getTypeCode();
                BigDecimal amount = txn.getAmount();
                
                switch (typeCode) {
                    case TYPE_PURCHASE:
                        totalPurchases = totalPurchases.add(amount);
                        break;
                    case TYPE_PAYMENT:
                        totalPayments = totalPayments.add(amount);
                        break;
                    case TYPE_CASH_ADVANCE:
                        totalCashAdvances = totalCashAdvances.add(amount);
                        break;
                    case TYPE_FEE:
                        totalFees = totalFees.add(amount);
                        break;
                    case TYPE_INTEREST:
                        totalInterest = totalInterest.add(amount);
                        break;
                }
            }
        }
        
        // Calculate balances
        BigDecimal previousBalance = account.getLastStatementBalance() != null 
            ? account.getLastStatementBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = account.getCurrentBalance();
        BigDecimal availableCredit = account.getCreditLimit().subtract(newBalance);
        
        // Calculate minimum payment due
        BigDecimal minimumPaymentDue = calculateMinimumPayment(newBalance);
        LocalDate paymentDueDate = statementDate.plusDays(PAYMENT_DUE_DAYS);
        
        // Create statement
        Statement statement = new Statement();
        statement.setStatementId(generateStatementId());
        statement.setAccountId(account.getAccountId());
        statement.setCustomerId(account.getCustomerId());
        statement.setStatementDate(statementDate);
        statement.setPeriodStart(periodStart);
        statement.setPeriodEnd(periodEnd);
        statement.setPreviousBalance(previousBalance);
        statement.setTotalPurchases(totalPurchases);
        statement.setTotalPayments(totalPayments);
        statement.setTotalCashAdvances(totalCashAdvances);
        statement.setTotalFees(totalFees);
        statement.setTotalInterest(totalInterest);
        statement.setNewBalance(newBalance);
        statement.setMinimumPaymentDue(minimumPaymentDue);
        statement.setPaymentDueDate(paymentDueDate);
        statement.setCreditLimit(account.getCreditLimit());
        statement.setAvailableCredit(availableCredit);
        statement.setGeneratedAt(LocalDateTime.now());
        statement.setStatus("GENERATED");
        
        statementRepository.save(statement);
        
        // Update account with statement info
        account.setLastStatementDate(statementDate);
        account.setLastStatementBalance(newBalance);
        account.setCurrentCycleCredit(BigDecimal.ZERO);
        account.setCurrentCycleDebit(BigDecimal.ZERO);
        accountRepository.save(account);
        
        log.info("Generated statement {} for account {}: balance={}, minPayment={}", 
            statement.getStatementId(), account.getAccountId(), newBalance, minimumPaymentDue);
        
        return StatementGenerationResult.builder()
            .statementId(statement.getStatementId())
            .accountId(account.getAccountId())
            .customerId(account.getCustomerId())
            .statementDate(statementDate)
            .newBalance(newBalance)
            .minimumPaymentDue(minimumPaymentDue)
            .paymentDueDate(paymentDueDate)
            .status("SUCCESS")
            .build();
    }
    
    private BigDecimal calculateMinimumPayment(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal percentagePayment = balance.multiply(MINIMUM_PAYMENT_PERCENTAGE)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Minimum payment is the greater of percentage or floor amount
        BigDecimal minimumPayment = percentagePayment.max(MINIMUM_PAYMENT_FLOOR);
        
        // But cannot exceed the balance
        return minimumPayment.min(balance);
    }
    
    private String generateStatementId() {
        return "STMT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
