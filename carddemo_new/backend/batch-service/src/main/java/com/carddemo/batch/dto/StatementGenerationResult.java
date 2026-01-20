package com.carddemo.batch.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementGenerationResult {
    private String statementId;
    private String accountId;
    private String customerId;
    private LocalDate statementDate;
    private BigDecimal newBalance;
    private BigDecimal minimumPaymentDue;
    private LocalDate paymentDueDate;
    private String status;
    private String errorMessage;
}
