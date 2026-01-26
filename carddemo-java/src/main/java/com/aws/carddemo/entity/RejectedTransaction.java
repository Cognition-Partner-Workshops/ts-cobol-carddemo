package com.aws.carddemo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rejected_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tran_id", nullable = false, length = 16)
    private String tranId;

    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNum;

    @Column(name = "tran_amt", precision = 12, scale = 2)
    private java.math.BigDecimal tranAmt;

    @Column(name = "tran_type_code", length = 2)
    private String tranTypeCode;

    @Column(name = "tran_orig_ts")
    private java.time.LocalDateTime tranOrigTs;

    @Column(name = "rejected_at")
    private java.time.LocalDateTime rejectedAt;

    @Column(name = "rejection_code", nullable = false)
    private Integer rejectionCode;

    @Column(name = "rejection_reason", nullable = false, length = 100)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public static final int INVALID_CARD_NUMBER = 100;
    public static final int ACCOUNT_NOT_FOUND = 101;
    public static final int OVERLIMIT_TRANSACTION = 102;
    public static final int ACCOUNT_EXPIRED = 103;

    public static final String INVALID_CARD_NUMBER_DESC = "INVALID CARD NUMBER FOUND";
    public static final String ACCOUNT_NOT_FOUND_DESC = "ACCOUNT RECORD NOT FOUND";
    public static final String OVERLIMIT_TRANSACTION_DESC = "OVERLIMIT TRANSACTION";
    public static final String ACCOUNT_EXPIRED_DESC = "TRANSACTION RECEIVED AFTER ACCT EXPIRATION";
}
