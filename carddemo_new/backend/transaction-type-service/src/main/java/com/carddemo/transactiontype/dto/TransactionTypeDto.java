package com.carddemo.transactiontype.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTypeDto {
    private String typeCode;
    private String typeDescription;
    private String debitCreditIndicator;
    private String categoryCode;
    private Boolean affectsBalance;
    private Boolean requiresApproval;
    private BigDecimal maxAmount;
    private BigDecimal minAmount;
    private BigDecimal feePercentage;
    private BigDecimal flatFee;
    private Boolean active;
    private Integer displayOrder;
}
