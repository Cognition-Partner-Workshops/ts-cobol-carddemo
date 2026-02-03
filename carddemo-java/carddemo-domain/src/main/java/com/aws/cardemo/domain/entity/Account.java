package com.aws.cardemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Entity representing a credit card account in the CardDemo system.
 * 
 * This entity maps to the 'accounts' table and stores all account-related information
 * including balance, credit limit, and billing cycle data. It represents the modernized
 * version of the COBOL ACCTDATA-RECORD from the original mainframe application.
 * 
 * Account status codes:
 * - 'A' = Active
 * - 'C' = Closed
 * - 'S' = Suspended
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    /**
     * Unique account identifier (primary key).
     * Maximum length: 11 characters.
     */
    @Id
    @Column(name = "account_id", length = 11)
    private String accountId;

    /**
     * Current status of the account.
     * Valid values: 'A' (Active), 'C' (Closed), 'S' (Suspended).
     */
    @NotNull
    @Column(name = "account_status", length = 1)
    private String accountStatus;

    /**
     * Current balance on the account.
     * Positive values indicate amount owed by the customer.
     */
    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    /**
     * Maximum credit limit for the account.
     * Transactions exceeding this limit may be declined.
     */
    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    /**
     * Date when the account was opened.
     */
    @Column(name = "open_date")
    private LocalDate openDate;

    /**
     * Date when the account expires.
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Date when the account was last reissued.
     */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    /**
     * Total credits (payments) applied during the current billing cycle.
     */
    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    private BigDecimal currentCycleCredit;

    /**
     * Total debits (purchases) applied during the current billing cycle.
     */
    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    private BigDecimal currentCycleDebit;

    /**
     * Group identifier for account categorization and reporting.
     */
    @Column(name = "group_id", length = 10)
    private String groupId;
}
