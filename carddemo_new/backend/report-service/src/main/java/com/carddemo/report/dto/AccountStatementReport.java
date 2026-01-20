package com.carddemo.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatementReport {
    private String accountId;
    private String customerName;
    private LocalDate statementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal previousBalance;
    private BigDecimal totalPurchases;
    private BigDecimal totalPayments;
    private BigDecimal totalFees;
    private BigDecimal totalInterest;
    private BigDecimal newBalance;
    private BigDecimal minimumPaymentDue;
    private LocalDate paymentDueDate;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private List<StatementTransaction> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementTransaction {
        private LocalDate transactionDate;
        private LocalDate postDate;
        private String description;
        private String referenceNumber;
        private BigDecimal amount;
    }
}
