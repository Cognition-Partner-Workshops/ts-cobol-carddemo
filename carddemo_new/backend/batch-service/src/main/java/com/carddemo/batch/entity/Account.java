package com.carddemo.batch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "active_status", length = 1)
    private String activeStatus;

    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    private BigDecimal currentCycleCredit;

    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    private BigDecimal currentCycleDebit;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "last_statement_date")
    private LocalDate lastStatementDate;

    @Column(name = "last_statement_balance", precision = 12, scale = 2)
    private BigDecimal lastStatementBalance;

    @Column(name = "customer_id", length = 9)
    private String customerId;
}
