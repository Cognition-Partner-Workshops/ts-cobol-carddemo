package com.carddemo.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Statement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_id", length = 20)
    private String statementId;

    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "customer_id", length = 9)
    private String customerId;

    @Column(name = "statement_date")
    private LocalDate statementDate;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "previous_balance", precision = 12, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "total_purchases", precision = 12, scale = 2)
    private BigDecimal totalPurchases;

    @Column(name = "total_payments", precision = 12, scale = 2)
    private BigDecimal totalPayments;

    @Column(name = "total_cash_advances", precision = 12, scale = 2)
    private BigDecimal totalCashAdvances;

    @Column(name = "total_fees", precision = 12, scale = 2)
    private BigDecimal totalFees;

    @Column(name = "total_interest", precision = 12, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "new_balance", precision = 12, scale = 2)
    private BigDecimal newBalance;

    @Column(name = "minimum_payment_due", precision = 12, scale = 2)
    private BigDecimal minimumPaymentDue;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "available_credit", precision = 12, scale = 2)
    private BigDecimal availableCredit;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "status", length = 20)
    private String status;
}
