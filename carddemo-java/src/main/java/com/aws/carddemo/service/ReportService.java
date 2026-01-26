package com.aws.carddemo.service;

import com.aws.carddemo.entity.*;
import com.aws.carddemo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final RejectedTransactionRepository rejectedTransactionRepository;
    private final InterestCalculationRepository interestCalculationRepository;

    public ReportService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          CardRepository cardRepository,
                          TransactionRepository transactionRepository,
                          RejectedTransactionRepository rejectedTransactionRepository,
                          InterestCalculationRepository interestCalculationRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.rejectedTransactionRepository = rejectedTransactionRepository;
        this.interestCalculationRepository = interestCalculationRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateDailySummaryReport(LocalDate date) {
        Map<String, Object> report = new HashMap<>();
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        List<Transaction> transactions = transactionRepository.findByDateRange(startOfDay, endOfDay);
        
        BigDecimal totalCredits = transactions.stream()
                .filter(Transaction::isCredit)
                .map(Transaction::getTranAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDebits = transactions.stream()
                .filter(Transaction::isDebit)
                .map(Transaction::getTranAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.put("date", date);
        report.put("totalTransactions", transactions.size());
        report.put("totalCredits", totalCredits);
        report.put("totalDebits", totalDebits);
        report.put("netAmount", totalCredits.add(totalDebits));
        
        List<RejectedTransaction> rejections = rejectedTransactionRepository
                .findByDateRange(startOfDay, endOfDay);
        report.put("totalRejections", rejections.size());
        
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateAccountSummaryReport(Long acctId) {
        Map<String, Object> report = new HashMap<>();
        
        Account account = accountRepository.findById(acctId).orElse(null);
        if (account == null) {
            report.put("error", "Account not found");
            return report;
        }
        
        report.put("acctId", account.getAcctId());
        report.put("status", account.getAcctActiveStatus());
        report.put("currentBalance", account.getAcctCurrBal());
        report.put("creditLimit", account.getAcctCreditLimit());
        report.put("availableCredit", account.getAvailableCredit());
        report.put("cycleCredit", account.getAcctCurrCycCredit());
        report.put("cycleDebit", account.getAcctCurrCycDebit());
        report.put("openDate", account.getAcctOpenDate());
        report.put("expirationDate", account.getAcctExpirationDate());
        
        List<Card> cards = cardRepository.findByAccountAcctId(acctId);
        report.put("totalCards", cards.size());
        report.put("activeCards", cards.stream().filter(Card::isActive).count());
        
        BigDecimal totalTransactions = transactionRepository.sumTransactionsByAccount(acctId);
        report.put("totalTransactionAmount", totalTransactions != null ? totalTransactions : BigDecimal.ZERO);
        
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateRejectionSummaryReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        
        List<RejectedTransaction> rejections = rejectedTransactionRepository.findByDateRange(start, end);
        
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalRejections", rejections.size());
        
        Map<Integer, Long> rejectionsByCode = new HashMap<>();
        for (RejectedTransaction rejection : rejections) {
            rejectionsByCode.merge(rejection.getRejectionCode(), 1L, Long::sum);
        }
        report.put("rejectionsByCode", rejectionsByCode);
        
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateSystemStatisticsReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("totalAccounts", accountRepository.count());
        report.put("activeAccounts", accountRepository.countByStatus("Y"));
        report.put("totalCustomers", customerRepository.count());
        report.put("totalCards", cardRepository.count());
        report.put("activeCards", cardRepository.countByStatus("Y"));
        report.put("totalTransactions", transactionRepository.count());
        
        List<Account> overlimitAccounts = accountRepository.findOverlimitAccounts();
        report.put("overlimitAccounts", overlimitAccounts.size());
        
        List<Account> expiredAccounts = accountRepository.findExpiredAccounts(LocalDate.now());
        report.put("expiredAccounts", expiredAccounts.size());
        
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateInterestReport(LocalDate calcDate) {
        Map<String, Object> report = new HashMap<>();
        
        List<InterestCalculation> calculations = interestCalculationRepository.findByCalcDate(calcDate);
        
        BigDecimal totalInterest = calculations.stream()
                .map(InterestCalculation::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.put("calcDate", calcDate);
        report.put("totalCalculations", calculations.size());
        report.put("totalInterestAmount", totalInterest);
        
        return report;
    }
}
