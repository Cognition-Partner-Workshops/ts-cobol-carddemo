package com.carddemo.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @Column(name = "payment_id", length = 16)
    private String paymentId;

    @Column(name = "account_id", length = 11)
    private String accountId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_source", length = 20)
    private String paymentSource;

    @Column(name = "source_account", length = 20)
    private String sourceAccount;

    @Column(name = "confirmation_number", length = 20)
    private String confirmationNumber;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
