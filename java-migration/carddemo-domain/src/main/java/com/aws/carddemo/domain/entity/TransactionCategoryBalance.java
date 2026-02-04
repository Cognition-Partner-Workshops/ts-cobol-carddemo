package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Transaction Category Balance entity - migrated from CVTRA01Y.cpy
 * Original VSAM record length: 50 bytes
 * Tracks balance by transaction category for each account
 */
@Entity
@Table(name = "transaction_category_balances", indexes = {
    @Index(name = "idx_cat_bal_account", columnList = "account_id"),
    @Index(name = "idx_cat_bal_type", columnList = "transactionTypeCode")
})
@IdClass(TransactionCategoryBalanceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCategoryBalance {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Id
    @NotNull
    @Size(max = 2)
    @Column(name = "transaction_type_code", length = 2, nullable = false)
    private String transactionTypeCode;

    @Id
    @NotNull
    @Column(name = "transaction_category_code", nullable = false)
    private Integer transactionCategoryCode;

    @NotNull
    @Column(name = "balance", precision = 11, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

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

    public void addToBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void subtractFromBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}
