package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Authorization detail entity mapped from IMS segment PAUTDTL1 (copybook CIPAUDTY).
 * Original IMS database: DBPAUTP0, child segment of PAUTSUM0.
 * Uses surrogate key (auto-generated ID) since IMS segments use positional keys.
 *
 * Match status values (88-level conditions):
 *   'P' = Pending (PA-MATCH-PENDING)
 *   'D' = Auth Declined (PA-MATCH-AUTH-DECLINED)
 *   'E' = Pending Expired (PA-MATCH-PENDING-EXPIRED)
 *   'M' = Matched with Transaction (PA-MATCHED-WITH-TRAN)
 *
 * Fraud status values:
 *   'F' = Fraud Confirmed (PA-FRAUD-CONFIRMED)
 *   'R' = Fraud Removed (PA-FRAUD-REMOVED)
 */
@Entity
@Table(name = "authorization_detail", indexes = {
        @Index(name = "idx_auth_detail_acct_id", columnList = "acct_id"),
        @Index(name = "idx_auth_detail_card_num", columnList = "card_num"),
        @Index(name = "idx_auth_detail_match_status", columnList = "match_status"),
        @Index(name = "idx_auth_detail_auth_date", columnList = "auth_date DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "auth_date")
    private LocalDate authDate;

    @Column(name = "auth_time")
    private LocalTime authTime;

    @Column(name = "auth_orig_date", length = 6)
    private String authOrigDate;

    @Column(name = "auth_orig_time", length = 6)
    private String authOrigTime;

    @Column(name = "card_num", length = 16)
    private String cardNum;

    @Column(name = "auth_type", length = 4)
    private String authType;

    @Column(name = "card_expiry_date", length = 4)
    private String cardExpiryDate;

    @Column(name = "message_type", length = 6)
    private String messageType;

    @Column(name = "message_source", length = 6)
    private String messageSource;

    @Column(name = "auth_id_code", length = 6)
    private String authIdCode;

    @Column(name = "auth_resp_code", length = 2)
    private String authRespCode;

    @Column(name = "auth_resp_reason", length = 4)
    private String authRespReason;

    @Column(name = "processing_code")
    private Integer processingCode;

    @Column(name = "transaction_amount", precision = 12, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "acquirer_country_code", length = 3)
    private String acquirerCountryCode;

    @Column(name = "pos_entry_mode")
    private Integer posEntryMode;

    @Column(name = "merchant_id", length = 15)
    private String merchantId;

    @Column(name = "merchant_name", length = 22)
    private String merchantName;

    @Column(name = "merchant_city", length = 13)
    private String merchantCity;

    @Column(name = "merchant_state", length = 2)
    private String merchantState;

    @Column(name = "merchant_zip", length = 9)
    private String merchantZip;

    @Column(name = "transaction_id", length = 15)
    private String transactionId;

    @Column(name = "match_status", length = 1)
    private String matchStatus;

    @Column(name = "auth_fraud", length = 1)
    private String authFraud;

    @Column(name = "fraud_report_date")
    private LocalDate fraudReportDate;
}
