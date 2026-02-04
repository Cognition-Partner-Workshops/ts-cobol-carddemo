package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Card entity - migrated from CVACT02Y.cpy
 * Original VSAM record length: 150 bytes
 */
@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_account", columnList = "account_id"),
    @Index(name = "idx_card_status", columnList = "activeStatus"),
    @Index(name = "idx_card_expiry", columnList = "expirationDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @Size(min = 16, max = 16)
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotNull
    @Min(100)
    @Max(999)
    @Column(name = "cvv_code", nullable = false)
    private Integer cvvCode;

    @NotBlank
    @Size(max = 50)
    @Column(name = "embossed_name", length = 50, nullable = false)
    private String embossedName;

    @NotNull
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @NotNull
    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus;

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

    public boolean isExpired() {
        return expirationDate.isBefore(LocalDate.now());
    }

    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 16) {
            return "****-****-****-****";
        }
        return "****-****-****-" + cardNumber.substring(12);
    }
}
