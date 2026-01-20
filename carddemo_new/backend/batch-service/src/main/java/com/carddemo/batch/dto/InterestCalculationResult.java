package com.carddemo.batch.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestCalculationResult {
    private String accountId;
    private BigDecimal previousBalance;
    private BigDecimal interestRate;
    private BigDecimal interestAmount;
    private BigDecimal newBalance;
    private String status;
    private String errorMessage;
}
