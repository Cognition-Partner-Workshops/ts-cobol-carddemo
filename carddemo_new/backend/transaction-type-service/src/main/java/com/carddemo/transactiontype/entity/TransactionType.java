package com.carddemo.transactiontype.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionType {
    @Id
    @Column(name = "type_code", length = 2)
    private String typeCode;

    @Column(name = "type_description", length = 50)
    private String typeDescription;

    @Column(name = "debit_credit_indicator", length = 1)
    private String debitCreditIndicator;

    @Column(name = "category_code", length = 4)
    private String categoryCode;

    @Column(name = "affects_balance")
    private Boolean affectsBalance;

    @Column(name = "requires_approval")
    private Boolean requiresApproval;

    @Column(name = "max_amount", precision = 12, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "min_amount", precision = 12, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "fee_percentage", precision = 5, scale = 2)
    private BigDecimal feePercentage;

    @Column(name = "flat_fee", precision = 12, scale = 2)
    private BigDecimal flatFee;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "display_order")
    private Integer displayOrder;
}
