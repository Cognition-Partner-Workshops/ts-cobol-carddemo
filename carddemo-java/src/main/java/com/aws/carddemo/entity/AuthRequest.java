package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNum;

    @Column(name = "tran_amt", nullable = false, precision = 11, scale = 2)
    private BigDecimal tranAmt;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "auth_status", nullable = false, length = 20)
    @Builder.Default
    private String authStatus = "PENDING";

    @Column(name = "auth_code", length = 10)
    private String authCode;

    @Column(name = "decline_reason", length = 100)
    private String declineReason;

    @Column(name = "request_ts", nullable = false)
    @Builder.Default
    private LocalDateTime requestTs = LocalDateTime.now();

    @Column(name = "response_ts")
    private LocalDateTime responseTs;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_ERROR = "ERROR";

    public boolean isPending() {
        return STATUS_PENDING.equals(this.authStatus);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(this.authStatus);
    }

    public boolean isDeclined() {
        return STATUS_DECLINED.equals(this.authStatus);
    }
}
