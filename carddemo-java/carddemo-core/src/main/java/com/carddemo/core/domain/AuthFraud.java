package com.carddemo.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Auth fraud tracking entity mapped from DB2 table AUTHFRDS.
 * Original DB2 table in the authorization IMS-DB2-MQ module.
 *
 * Records flagged as fraudulent during authorization review (COPAUS1C/COPAUS2C).
 */
@Entity
@Table(name = "auth_fraud",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_fraud_card_ts",
                        columnNames = {"card_num", "auth_timestamp"})
        },
        indexes = {
                @Index(name = "idx_auth_fraud_acct_id", columnList = "acct_id"),
                @Index(name = "idx_auth_fraud_cust_id", columnList = "cust_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthFraud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "auth_timestamp", nullable = false)
    private LocalDateTime authTimestamp;

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

    @Column(name = "processing_code", length = 6)
    private String processingCode;

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
    private String authFraudFlag;

    @Column(name = "fraud_report_date")
    private LocalDate fraudReportDate;

    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cust_id")
    private Long custId;
}
