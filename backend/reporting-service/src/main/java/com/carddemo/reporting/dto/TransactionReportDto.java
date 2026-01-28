package com.carddemo.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionReportDto {
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private Long totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal averageTransactionAmount;
    private Map<String, Long> transactionsByType;
    private Map<String, BigDecimal> amountByType;
    private Map<String, Long> transactionsByDay;
    private Long purchaseCount;
    private Long paymentCount;
    private Long refundCount;
}
