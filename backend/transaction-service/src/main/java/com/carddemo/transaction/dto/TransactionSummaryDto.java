package com.carddemo.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummaryDto {
    private Long totalTransactions;
    private Long transactionsToday;
    private Long transactionsThisWeek;
    private Long transactionsThisMonth;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal netAmount;
}
