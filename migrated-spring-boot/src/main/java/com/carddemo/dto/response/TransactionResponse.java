package com.carddemo.dto.response;

import com.carddemo.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Transaction entity.
 * Used for returning transaction data in API responses.
 */
public class TransactionResponse {

    private String transactionId;
    private String typeCode;
    private Integer categoryCode;
    private String source;
    private String description;
    private BigDecimal amount;
    private Long merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private String cardNumber;
    private String maskedCardNumber;
    private LocalDateTime originationTimestamp;
    private LocalDateTime processingTimestamp;

    public TransactionResponse() {
    }

    public static TransactionResponse fromEntity(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setTypeCode(transaction.getTypeCode());
        response.setCategoryCode(transaction.getCategoryCode());
        response.setSource(transaction.getSource());
        response.setDescription(transaction.getDescription());
        response.setAmount(transaction.getAmount());
        response.setMerchantId(transaction.getMerchantId());
        response.setMerchantName(transaction.getMerchantName());
        response.setMerchantCity(transaction.getMerchantCity());
        response.setMerchantZip(transaction.getMerchantZip());
        response.setCardNumber(transaction.getCardNumber());
        response.setMaskedCardNumber(maskCardNumber(transaction.getCardNumber()));
        response.setOriginationTimestamp(transaction.getOriginationTimestamp());
        response.setProcessingTimestamp(transaction.getProcessingTimestamp());
        return response;
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
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

    public Integer getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(Integer categoryCode) {
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

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
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

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
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
