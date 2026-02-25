package com.cardemo.dto;

import java.math.BigDecimal;

/**
 * DTO for MQ authorization request processing.
 * Migrated from COPAUA0C (CP00 transaction) MQ trigger program.
 * Maps to the CSV input format on queue AWS.M2.CARDDEMO.PAUTH.REQUEST
 */
public class AuthorizationRequest {

    private String authDate;
    private String authTime;
    private String cardNum;
    private String authType;
    private String cardExpiryDate;
    private String messageType;
    private String messageSource;
    private String processingCode;
    private BigDecimal transactionAmt;
    private String merchantCategoryCode;
    private String acqrCountryCode;
    private Integer posEntryMode;
    private String merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantState;
    private String merchantZip;
    private String transactionId;

    public AuthorizationRequest() {
    }

    public String getAuthDate() { return authDate; }
    public void setAuthDate(String authDate) { this.authDate = authDate; }
    public String getAuthTime() { return authTime; }
    public void setAuthTime(String authTime) { this.authTime = authTime; }
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
    public String getProcessingCode() { return processingCode; }
    public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }
    public BigDecimal getTransactionAmt() { return transactionAmt; }
    public void setTransactionAmt(BigDecimal transactionAmt) { this.transactionAmt = transactionAmt; }
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
}
