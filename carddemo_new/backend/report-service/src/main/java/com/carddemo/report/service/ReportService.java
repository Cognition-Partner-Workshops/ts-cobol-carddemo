package com.carddemo.report.service;

import com.carddemo.report.dto.AccountStatementReport;
import com.carddemo.report.dto.TransactionSummaryReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    public TransactionSummaryReport generateTransactionSummary(String accountId, LocalDate startDate, LocalDate endDate) {
        List<TransactionSummaryReport.CategorySummary> categories = new ArrayList<>();
        categories.add(TransactionSummaryReport.CategorySummary.builder()
                .categoryCode("0001")
                .categoryDescription("Retail Purchase")
                .transactionCount(15)
                .totalAmount(new BigDecimal("1250.00"))
                .percentage(new BigDecimal("45.5"))
                .build());
        categories.add(TransactionSummaryReport.CategorySummary.builder()
                .categoryCode("0004")
                .categoryDescription("Dining")
                .transactionCount(8)
                .totalAmount(new BigDecimal("450.00"))
                .percentage(new BigDecimal("16.4"))
                .build());
        categories.add(TransactionSummaryReport.CategorySummary.builder()
                .categoryCode("0005")
                .categoryDescription("Fuel")
                .transactionCount(6)
                .totalAmount(new BigDecimal("320.00"))
                .percentage(new BigDecimal("11.6"))
                .build());

        BigDecimal totalPurchases = new BigDecimal("2750.00");
        BigDecimal totalPayments = new BigDecimal("2000.00");
        BigDecimal totalCashAdvances = new BigDecimal("200.00");
        BigDecimal totalFees = new BigDecimal("25.00");
        BigDecimal totalInterest = new BigDecimal("45.50");

        return TransactionSummaryReport.builder()
                .accountId(accountId)
                .startDate(startDate)
                .endDate(endDate)
                .totalTransactions(35)
                .totalPurchases(totalPurchases)
                .totalPayments(totalPayments)
                .totalCashAdvances(totalCashAdvances)
                .totalFees(totalFees)
                .totalInterest(totalInterest)
                .netChange(totalPurchases.add(totalCashAdvances).add(totalFees).add(totalInterest).subtract(totalPayments))
                .categoryBreakdown(categories)
                .build();
    }

    public AccountStatementReport generateAccountStatement(String accountId, int month, int year) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);

        List<AccountStatementReport.StatementTransaction> transactions = new ArrayList<>();
        transactions.add(AccountStatementReport.StatementTransaction.builder()
                .transactionDate(periodStart.plusDays(2))
                .postDate(periodStart.plusDays(3))
                .description("WALMART STORE #1234")
                .referenceNumber("TXN001")
                .amount(new BigDecimal("125.50"))
                .build());
        transactions.add(AccountStatementReport.StatementTransaction.builder()
                .transactionDate(periodStart.plusDays(5))
                .postDate(periodStart.plusDays(6))
                .description("STARBUCKS #5678")
                .referenceNumber("TXN002")
                .amount(new BigDecimal("15.75"))
                .build());
        transactions.add(AccountStatementReport.StatementTransaction.builder()
                .transactionDate(periodStart.plusDays(10))
                .postDate(periodStart.plusDays(11))
                .description("PAYMENT - THANK YOU")
                .referenceNumber("PMT001")
                .amount(new BigDecimal("-500.00"))
                .build());

        BigDecimal previousBalance = new BigDecimal("1500.00");
        BigDecimal totalPurchases = new BigDecimal("750.00");
        BigDecimal totalPayments = new BigDecimal("500.00");
        BigDecimal totalFees = new BigDecimal("0.00");
        BigDecimal totalInterest = new BigDecimal("22.50");
        BigDecimal newBalance = previousBalance.add(totalPurchases).subtract(totalPayments).add(totalFees).add(totalInterest);
        BigDecimal creditLimit = new BigDecimal("10000.00");

        return AccountStatementReport.builder()
                .accountId(accountId)
                .customerName("John M Smith")
                .statementDate(periodEnd)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .previousBalance(previousBalance)
                .totalPurchases(totalPurchases)
                .totalPayments(totalPayments)
                .totalFees(totalFees)
                .totalInterest(totalInterest)
                .newBalance(newBalance)
                .minimumPaymentDue(newBalance.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("25.00")))
                .paymentDueDate(periodEnd.plusDays(25))
                .creditLimit(creditLimit)
                .availableCredit(creditLimit.subtract(newBalance))
                .transactions(transactions)
                .build();
    }
}
