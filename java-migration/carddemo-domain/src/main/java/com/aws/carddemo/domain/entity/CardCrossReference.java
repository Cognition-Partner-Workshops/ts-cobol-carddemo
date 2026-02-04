package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Card Cross-Reference entity - migrated from CVACT03Y.cpy
 * Original VSAM record length: 50 bytes
 * Links cards to customers and accounts
 */
@Entity
@Table(name = "card_cross_references", indexes = {
    @Index(name = "idx_xref_customer", columnList = "customer_id"),
    @Index(name = "idx_xref_account", columnList = "account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardCrossReference {

    @Id
    @Size(min = 16, max = 16)
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

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
}
