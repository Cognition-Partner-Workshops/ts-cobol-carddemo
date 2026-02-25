package com.carddemo.service;

import com.carddemo.entity.Account;
import com.carddemo.entity.Transaction;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report service - migrated from CORPT00C (CR00 - Transaction Reports).
 *
 * CORPT00C logic:
 * 1. Accept report criteria from BMS map (date range, account, etc.)
 * 2. Browse TRANSACT file matching criteria
 * 3. Accumulate totals by transaction type
 * 4. Display report summary
 */
@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public ReportService(TransactionRepository transactionRepository,
                         AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Generate a transaction report for a specific card.
     * Replaces CORPT00C browse and accumulate logic.
     */
    public Map<String, Object> generateTransactionReport(String cardNum, String startDate, String endDate) {
        List<Transaction> transactions = transactionRepository
                .findByCardNumOrderByOrigTimestampDesc(cardNum);

        // Filter by date range if provided
        if (startDate != null && endDate != null) {
            transactions = transactions.stream()
                    .filter(t -> {
                        String ts = t.getOrigTimestamp();
                        if (ts == null || ts.length() < 10) return false;
                        String tranDate = ts.substring(0, 10);
                        return tranDate.compareTo(startDate) >= 0 && tranDate.compareTo(endDate) <= 0;
                    })
                    .collect(Collectors.toList());
        }

        // Compute totals by transaction type
        Map<String, BigDecimal> totalsByType = new HashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        int totalCount = 0;

        for (Transaction t : transactions) {
            String typeCd = t.getTranTypeCd() != null ? t.getTranTypeCd() : "??";
            BigDecimal amt = t.getTranAmt() != null ? t.getTranAmt() : BigDecimal.ZERO;
            totalsByType.merge(typeCd, amt, BigDecimal::add);
            grandTotal = grandTotal.add(amt);
            totalCount++;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("cardNum", cardNum);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("transactions", transactions);
        report.put("totalsByType", totalsByType);
        report.put("grandTotal", grandTotal);
        report.put("totalCount", totalCount);
        report.put("generatedDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return report;
    }

    /**
     * Generate an account summary report.
     */
    public Map<String, Object> generateAccountSummary(Long acctId) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        BigDecimal totalTransactions = transactionRepository.sumAmountByCardNum(null);

        Map<String, Object> summary = new HashMap<>();
        summary.put("account", account);
        summary.put("currentBalance", account.getCurrentBalance());
        summary.put("creditLimit", account.getCreditLimit());
        summary.put("availableCredit", account.getCreditLimit() != null && account.getCurrentBalance() != null
                ? account.getCreditLimit().subtract(account.getCurrentBalance())
                : BigDecimal.ZERO);
        return summary;
    }
}
