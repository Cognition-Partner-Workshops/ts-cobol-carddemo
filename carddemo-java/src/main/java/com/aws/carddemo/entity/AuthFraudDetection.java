package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_fraud_detection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthFraudDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fraud_id")
    private Long fraudId;

    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_id")
    private AuthRequest authRequest;

    @Column(name = "fraud_score", precision = 5, scale = 2)
    private BigDecimal fraudScore;

    @Column(name = "fraud_indicators", columnDefinition = "jsonb")
    private String fraudIndicators;

    @Column(name = "is_fraud", nullable = false)
    @Builder.Default
    private Boolean isFraud = false;

    @Column(name = "reviewed", nullable = false)
    @Builder.Default
    private Boolean reviewed = false;

    @Column(name = "reviewed_by", length = 8)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
