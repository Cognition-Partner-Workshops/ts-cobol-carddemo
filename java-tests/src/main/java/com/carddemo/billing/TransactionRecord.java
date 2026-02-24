package com.carddemo.billing;

import java.math.BigDecimal;

/**
 * Java equivalent of the COBOL TRAN-RECORD copybook (CVTRA05Y.cpy).
 *
 * <pre>
 * 01  TRAN-RECORD.
 *     05  TRAN-ID               PIC X(16).
 *     05  TRAN-TYPE-CD          PIC X(02).
 *     05  TRAN-CAT-CD           PIC 9(04).
 *     05  TRAN-SOURCE           PIC X(10).
 *     05  TRAN-DESC             PIC X(100).
 *     05  TRAN-AMT              PIC S9(09)V99.
 *     05  TRAN-MERCHANT-ID      PIC 9(09).
 *     05  TRAN-MERCHANT-NAME    PIC X(50).
 *     05  TRAN-MERCHANT-CITY    PIC X(50).
 *     05  TRAN-MERCHANT-ZIP     PIC X(10).
 *     05  TRAN-CARD-NUM         PIC X(16).
 *     05  TRAN-ORIG-TS          PIC X(26).
 *     05  TRAN-PROC-TS          PIC X(26).
 * </pre>
 */
public class TransactionRecord {

    private String transactionId;
    private String transactionTypeCode;
    private int transactionCategoryCode;
    private String transactionSource;
    private String transactionDescription;
    private BigDecimal transactionAmount;
    private long merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private String cardNumber;
    private String originTimestamp;
    private String processTimestamp;

    public TransactionRecord() {
        this.transactionAmount = BigDecimal.ZERO;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    public int getTransactionCategoryCode() {
        return transactionCategoryCode;
    }

    public void setTransactionCategoryCode(int transactionCategoryCode) {
        this.transactionCategoryCode = transactionCategoryCode;
    }

    public String getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(String transactionSource) {
        this.transactionSource = transactionSource;
    }

    public String getTransactionDescription() {
        return transactionDescription;
    }

    public void setTransactionDescription(String transactionDescription) {
        this.transactionDescription = transactionDescription;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
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

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getOriginTimestamp() {
        return originTimestamp;
    }

    public void setOriginTimestamp(String originTimestamp) {
        this.originTimestamp = originTimestamp;
    }

    public String getProcessTimestamp() {
        return processTimestamp;
    }

    public void setProcessTimestamp(String processTimestamp) {
        this.processTimestamp = processTimestamp;
    }
}
