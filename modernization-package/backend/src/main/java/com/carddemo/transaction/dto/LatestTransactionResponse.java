package com.carddemo.transaction.dto;

import java.math.BigDecimal;

/**
 * Data fields from the most recent transaction for PF5 Copy Last (US-AT-06).
 * Excludes Account ID and Card Number (key fields are NOT overwritten during copy).
 */
public class LatestTransactionResponse {

    private String transactionId;
    private String typeCode;
    private int categoryCode;
    private String source;
    private String description;
    private BigDecimal amount;
    private String originationDate;
    private String processingDate;
    private long merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;

    public LatestTransactionResponse() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public String getOriginationDate() {
        return originationDate;
    }

    public void setOriginationDate(String originationDate) {
        this.originationDate = originationDate;
    }

    public String getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(String processingDate) {
        this.processingDate = processingDate;
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
}
