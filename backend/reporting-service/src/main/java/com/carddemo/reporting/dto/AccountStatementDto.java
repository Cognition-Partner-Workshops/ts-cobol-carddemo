package com.carddemo.reporting.dto;

import com.carddemo.common.dto.TransactionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatementDto {
    private Long accountId;
    private String customerName;
    private LocalDate statementStartDate;
    private LocalDate statementEndDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal minimumPaymentDue;
    private LocalDate paymentDueDate;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private List<TransactionDto> transactions;
}
