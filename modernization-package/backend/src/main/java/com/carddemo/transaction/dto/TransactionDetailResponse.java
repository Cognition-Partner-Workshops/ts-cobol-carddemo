package com.carddemo.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Full transaction detail with all 13 fields (BR-VT-03).
 * Replaces the CT01 3270 detail screen.
 * Includes resolved Account ID from cross-reference for display.
 */
public class TransactionDetailResponse {

    private String transactionId;
    private String accountId;
    private String cardNumber;
    private String typeCode;
    private int categoryCode;
    private String source;
    private String description;
    private BigDecimal amount;
    private long merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private LocalDateTime originationTimestamp;
    private LocalDateTime processingTimestamp;

    public TransactionDetailResponse() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public int getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(int categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    public String getMerchantZip() {
        return merchantZip;
    }

    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    public LocalDateTime getOriginationTimestamp() {
        return originationTimestamp;
    }

    public void setOriginationTimestamp(LocalDateTime originationTimestamp) {
        this.originationTimestamp = originationTimestamp;
    }

    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }

    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }
}
