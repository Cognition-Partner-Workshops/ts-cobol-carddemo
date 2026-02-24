package com.carddemo.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Summary view of a transaction for the list screen (CT00).
 * Matches the columns displayed on the legacy 3270 list screen.
 */
public class TransactionSummaryDto {

    private String transactionId;
    private String typeCode;
    private int categoryCode;
    private String source;
    private String description;
    private BigDecimal amount;
    private String cardNumber;
    private LocalDateTime originationTimestamp;
    private LocalDateTime processingTimestamp;

    public TransactionSummaryDto() {
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

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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
