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
public class TransactionSummaryReport {
    private String accountId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalTransactions;
    private BigDecimal totalPurchases;
    private BigDecimal totalPayments;
    private BigDecimal totalCashAdvances;
    private BigDecimal totalFees;
    private BigDecimal totalInterest;
    private BigDecimal netChange;
    private List<CategorySummary> categoryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String categoryCode;
        private String categoryDescription;
        private int transactionCount;
        private BigDecimal totalAmount;
        private BigDecimal percentage;
    }
}
