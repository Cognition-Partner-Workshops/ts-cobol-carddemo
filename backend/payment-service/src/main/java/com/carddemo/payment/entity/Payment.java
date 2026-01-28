package com.carddemo.payment.entity;

import com.carddemo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @Column(name = "payment_id", length = 16)
    private String paymentId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 20, nullable = false)
    private String paymentMethod;

    @Column(name = "source_account", length = 20)
    private String sourceAccount;

    @Column(name = "routing_number", length = 9)
    private String routingNumber;

    @Column(name = "confirmation_number", length = 20)
    private String confirmationNumber;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "notes", length = 255)
    private String notes;
}
