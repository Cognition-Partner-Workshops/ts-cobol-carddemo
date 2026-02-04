package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Account entity - migrated from CVACT01Y.cpy
 * Original VSAM record length: 300 bytes
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_status", columnList = "activeStatus"),
    @Index(name = "idx_account_group", columnList = "groupId"),
    @Index(name = "idx_account_zip", columnList = "zipCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @NotNull
    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus;

    @NotNull
    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal currentBalance;

    @NotNull
    @Column(name = "credit_limit", precision = 12, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @NotNull
    @Column(name = "cash_credit_limit", precision = 12, scale = 2, nullable = false)
    private BigDecimal cashCreditLimit;

    @NotNull
    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal currentCycleCredit = BigDecimal.ZERO;

    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal currentCycleDebit = BigDecimal.ZERO;

    @Size(max = 10)
    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Size(max = 10)
    @Column(name = "group_id", length = 10)
    private String groupId;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Card> cards = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CardCrossReference> cardCrossReferences = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionCategoryBalance> categoryBalances = new ArrayList<>();

    @Version
    private Long version;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    public boolean isActive() {
        return "Y".equalsIgnoreCase(activeStatus);
    }

    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(currentBalance);
    }

    public BigDecimal getAvailableCashCredit() {
        return cashCreditLimit.subtract(currentBalance);
    }

    public boolean isOverLimit() {
        return currentBalance.compareTo(creditLimit) > 0;
    }
}
