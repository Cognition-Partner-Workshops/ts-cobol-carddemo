package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Authorization Detail entity - migrated from IMS segment PAUTDTL1 (child of PAUTSUM0) in DBPAUTP0.
 * Flattened from hierarchical IMS DB to relational table with FK to authorization_summary.
 * Original IMS segment: 200 bytes.
 */
@Entity
@Table(name = "authorization_details")
public class AuthorizationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "auth_date", length = 6)
    @Size(max = 6)
    private String authDate;

    @Column(name = "auth_time", length = 6)
    @Size(max = 6)
    private String authTime;

    @Column(name = "auth_orig_date", length = 6)
    @Size(max = 6)
    private String authOrigDate;

    @Column(name = "auth_orig_time", length = 6)
    @Size(max = 6)
    private String authOrigTime;

    @Column(name = "card_num", length = 16)
    @Size(max = 16)
    private String cardNum;

    @Column(name = "auth_type", length = 4)
    @Size(max = 4)
    private String authType;

    @Column(name = "card_expiry_date", length = 4)
    @Size(max = 4)
    private String cardExpiryDate;

    @Column(name = "message_type", length = 6)
    @Size(max = 6)
    private String messageType;

    @Column(name = "message_source", length = 6)
    @Size(max = 6)
    private String messageSource;

    @Column(name = "auth_id_code", length = 6)
    @Size(max = 6)
    private String authIdCode;

    @Column(name = "auth_resp_code", length = 2)
    @Size(max = 2)
    private String authRespCode;

    @Column(name = "auth_resp_reason", length = 4)
    @Size(max = 4)
    private String authRespReason;

    @Column(name = "processing_code")
    private Integer processingCode;

    @Column(name = "transaction_amt", precision = 12, scale = 2)
    private BigDecimal transactionAmt;

    @Column(name = "approved_amt", precision = 12, scale = 2)
    private BigDecimal approvedAmt;

    @Column(name = "merchant_category_code", length = 4)
    @Size(max = 4)
    private String merchantCategoryCode;

    @Column(name = "acqr_country_code", length = 3)
    @Size(max = 3)
    private String acqrCountryCode;

    @Column(name = "pos_entry_mode")
    private Integer posEntryMode;

    @Column(name = "merchant_id", length = 15)
    @Size(max = 15)
    private String merchantId;

    @Column(name = "merchant_name", length = 22)
    @Size(max = 22)
    private String merchantName;

    @Column(name = "merchant_city", length = 13)
    @Size(max = 13)
    private String merchantCity;

    @Column(name = "merchant_state", length = 2)
    @Size(max = 2)
    private String merchantState;

    @Column(name = "merchant_zip", length = 9)
    @Size(max = 9)
    private String merchantZip;

    @Column(name = "transaction_id", length = 15)
    @Size(max = 15)
    private String transactionId;

    @Column(name = "match_status", length = 1)
    @Size(max = 1)
    private String matchStatus;

    @Column(name = "auth_fraud", length = 1)
    @Size(max = 1)
    private String authFraud;

    @Column(name = "fraud_rpt_date", length = 8)
    @Size(max = 8)
    private String fraudRptDate;

    public AuthorizationDetail() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSummaryId() { return summaryId; }
    public void setSummaryId(Long summaryId) { this.summaryId = summaryId; }
    public String getAuthDate() { return authDate; }
    public void setAuthDate(String authDate) { this.authDate = authDate; }
    public String getAuthTime() { return authTime; }
    public void setAuthTime(String authTime) { this.authTime = authTime; }
    public String getAuthOrigDate() { return authOrigDate; }
    public void setAuthOrigDate(String authOrigDate) { this.authOrigDate = authOrigDate; }
    public String getAuthOrigTime() { return authOrigTime; }
    public void setAuthOrigTime(String authOrigTime) { this.authOrigTime = authOrigTime; }
    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getCardExpiryDate() { return cardExpiryDate; }
    public void setCardExpiryDate(String cardExpiryDate) { this.cardExpiryDate = cardExpiryDate; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getMessageSource() { return messageSource; }
    public void setMessageSource(String messageSource) { this.messageSource = messageSource; }
    public String getAuthIdCode() { return authIdCode; }
    public void setAuthIdCode(String authIdCode) { this.authIdCode = authIdCode; }
    public String getAuthRespCode() { return authRespCode; }
    public void setAuthRespCode(String authRespCode) { this.authRespCode = authRespCode; }
    public String getAuthRespReason() { return authRespReason; }
    public void setAuthRespReason(String authRespReason) { this.authRespReason = authRespReason; }
    public Integer getProcessingCode() { return processingCode; }
    public void setProcessingCode(Integer processingCode) { this.processingCode = processingCode; }
    public BigDecimal getTransactionAmt() { return transactionAmt; }
    public void setTransactionAmt(BigDecimal transactionAmt) { this.transactionAmt = transactionAmt; }
    public BigDecimal getApprovedAmt() { return approvedAmt; }
    public void setApprovedAmt(BigDecimal approvedAmt) { this.approvedAmt = approvedAmt; }
    public String getMerchantCategoryCode() { return merchantCategoryCode; }
    public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }
    public String getAcqrCountryCode() { return acqrCountryCode; }
    public void setAcqrCountryCode(String acqrCountryCode) { this.acqrCountryCode = acqrCountryCode; }
    public Integer getPosEntryMode() { return posEntryMode; }
    public void setPosEntryMode(Integer posEntryMode) { this.posEntryMode = posEntryMode; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMerchantCity() { return merchantCity; }
    public void setMerchantCity(String merchantCity) { this.merchantCity = merchantCity; }
    public String getMerchantState() { return merchantState; }
    public void setMerchantState(String merchantState) { this.merchantState = merchantState; }
    public String getMerchantZip() { return merchantZip; }
    public void setMerchantZip(String merchantZip) { this.merchantZip = merchantZip; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
    public String getAuthFraud() { return authFraud; }
    public void setAuthFraud(String authFraud) { this.authFraud = authFraud; }
    public String getFraudRptDate() { return fraudRptDate; }
    public void setFraudRptDate(String fraudRptDate) { this.fraudRptDate = fraudRptDate; }

    public boolean isApproved() { return "00".equals(authRespCode); }
    public boolean isMatchPending() { return "P".equals(matchStatus); }
    public boolean isMatchDeclined() { return "D".equals(matchStatus); }
    public boolean isPendingExpired() { return "E".equals(matchStatus); }
    public boolean isMatchedWithTran() { return "M".equals(matchStatus); }
    public boolean isFraudConfirmed() { return "F".equals(authFraud); }
    public boolean isFraudRemoved() { return "R".equals(authFraud); }
}
