package com.carddemo.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private Long totalCustomers;
    private Long totalAccounts;
    private Long activeAccounts;
    private Long totalCards;
    private Long activeCards;
    private Long totalTransactionsToday;
    private Long totalTransactionsThisMonth;
    private BigDecimal totalBalanceOutstanding;
    private BigDecimal totalCreditLimit;
    private BigDecimal utilizationRate;
    private Long overLimitAccounts;
    private Long expiringCardsNext30Days;
}
