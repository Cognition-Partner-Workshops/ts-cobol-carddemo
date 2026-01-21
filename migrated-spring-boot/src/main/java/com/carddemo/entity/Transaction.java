package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a credit card transaction.
 * Migrated from mainframe copybook: CVTRA05Y.cpy (TRAN-RECORD)
 *
 * <p>This entity maps the VSAM transaction file structure to a relational database table.
 * Transaction amounts use BigDecimal for precision. Timestamps are stored as LocalDateTime.
 *
 * @see com.carddemo.repository.TransactionRepository
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Size(max = 16)
    @Column(name = "transaction_id", length = 16)
    private String transactionId;

    @NotNull
    @Size(max = 2)
    @Column(name = "type_code", length = 2, nullable = false)
    private String typeCode;

    @NotNull
    @Column(name = "category_code", nullable = false)
    private Integer categoryCode;

    @Size(max = 10)
    @Column(name = "source", length = 10)
    private String source;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @NotNull
    @Column(name = "amount", precision = 11, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Size(max = 50)
    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Size(max = 50)
    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Size(max = 10)
    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @NotNull
    @Size(max = 16)
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "origination_timestamp")
    private LocalDateTime originationTimestamp;

    @Column(name = "processing_timestamp")
    private LocalDateTime processingTimestamp;

    public Transaction() {
    }

    public Transaction(String transactionId, String typeCode, Integer categoryCode,
                       BigDecimal amount, String cardNumber) {
        this.transactionId = transactionId;
        this.typeCode = typeCode;
        this.categoryCode = categoryCode;
        this.amount = amount;
        this.cardNumber = cardNumber;
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

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", typeCode='" + typeCode + '\'' +
                ", amount=" + amount +
                ", cardNumber='" + cardNumber + '\'' +
                '}';
    }
}
