package com.carddemo.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountSummaryDto {
    private Long totalAccounts;
    private Long activeAccounts;
    private Long inactiveAccounts;
    private BigDecimal totalBalance;
    private BigDecimal totalCreditLimit;
    private BigDecimal averageBalance;
    private Long overLimitAccounts;
}
