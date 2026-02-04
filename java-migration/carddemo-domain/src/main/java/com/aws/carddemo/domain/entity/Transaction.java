package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity - migrated from CVTRA05Y.cpy
 * Original VSAM record length: 350 bytes
 * Has alternate index on card number (TRANSACT AIX)
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_card", columnList = "cardNumber"),
    @Index(name = "idx_transaction_type", columnList = "transactionTypeCode"),
    @Index(name = "idx_transaction_category", columnList = "transactionCategoryCode"),
    @Index(name = "idx_transaction_orig_ts", columnList = "originTimestamp"),
    @Index(name = "idx_transaction_merchant", columnList = "merchantId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Size(max = 16)
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    @NotNull
    @Size(max = 2)
    @Column(name = "transaction_type_code", length = 2, nullable = false)
    private String transactionTypeCode;

    @NotNull
    @Column(name = "transaction_category_code", nullable = false)
    private Integer transactionCategoryCode;

    @Size(max = 10)
    @Column(name = "transaction_source", length = 10)
    private String transactionSource;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @NotNull
    @Column(name = "amount", precision = 11, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Size(max = 50)
    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Size(max = 50)
    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Size(max = 10)
    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @NotNull
    @Size(min = 16, max = 16)
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @NotNull
    @Column(name = "origin_timestamp", nullable = false)
    private LocalDateTime originTimestamp;

    @Column(name = "process_timestamp")
    private LocalDateTime processTimestamp;

    @Version
    private Long version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (processTimestamp == null) {
            processTimestamp = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isCredit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isDebit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }
}
