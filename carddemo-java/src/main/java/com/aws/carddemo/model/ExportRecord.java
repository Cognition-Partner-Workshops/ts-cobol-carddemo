package com.aws.carddemo.model;

import java.time.LocalDateTime;

/**
 * Export Record - migrated from COBOL copybook CVEXPORT.cpy
 * Multi-record export layout for branch migration
 * Total Record Length: 500 bytes
 * Record types: C=Customer, A=Account, X=Cross-ref, T=Transaction, D=Card
 */
public class ExportRecord {

    public static final char RECORD_TYPE_CUSTOMER = 'C';
    public static final char RECORD_TYPE_ACCOUNT = 'A';
    public static final char RECORD_TYPE_XREF = 'X';
    public static final char RECORD_TYPE_TRANSACTION = 'T';
    public static final char RECORD_TYPE_CARD = 'D';

    private char exportRecType;
    private String exportTimestamp;
    private Long exportSequenceNum;
    private String exportBranchId;
    private String exportRegionCode;

    private Customer customerData;
    private Account accountData;
    private Transaction transactionData;
    private CardXref cardXrefData;
    private Card cardData;

    public ExportRecord() {
    }

    public char getExportRecType() {
        return exportRecType;
    }

    public void setExportRecType(char exportRecType) {
        this.exportRecType = exportRecType;
    }

    public String getExportTimestamp() {
        return exportTimestamp;
    }

    public void setExportTimestamp(String exportTimestamp) {
        this.exportTimestamp = exportTimestamp;
    }

    public Long getExportSequenceNum() {
        return exportSequenceNum;
    }

    public void setExportSequenceNum(Long exportSequenceNum) {
        this.exportSequenceNum = exportSequenceNum;
    }

    public String getExportBranchId() {
        return exportBranchId;
    }

    public void setExportBranchId(String exportBranchId) {
        this.exportBranchId = exportBranchId;
    }

    public String getExportRegionCode() {
        return exportRegionCode;
    }

    public void setExportRegionCode(String exportRegionCode) {
        this.exportRegionCode = exportRegionCode;
    }

    public Customer getCustomerData() {
        return customerData;
    }

    public void setCustomerData(Customer customerData) {
        this.customerData = customerData;
    }

    public Account getAccountData() {
        return accountData;
    }

    public void setAccountData(Account accountData) {
        this.accountData = accountData;
    }

    public Transaction getTransactionData() {
        return transactionData;
    }

    public void setTransactionData(Transaction transactionData) {
        this.transactionData = transactionData;
    }

    public CardXref getCardXrefData() {
        return cardXrefData;
    }

    public void setCardXrefData(CardXref cardXrefData) {
        this.cardXrefData = cardXrefData;
    }

    public Card getCardData() {
        return cardData;
    }

    public void setCardData(Card cardData) {
        this.cardData = cardData;
    }

    public boolean isCustomerRecord() {
        return exportRecType == RECORD_TYPE_CUSTOMER;
    }

    public boolean isAccountRecord() {
        return exportRecType == RECORD_TYPE_ACCOUNT;
    }

    public boolean isXrefRecord() {
        return exportRecType == RECORD_TYPE_XREF;
    }

    public boolean isTransactionRecord() {
        return exportRecType == RECORD_TYPE_TRANSACTION;
    }

    public boolean isCardRecord() {
        return exportRecType == RECORD_TYPE_CARD;
    }

    @Override
    public String toString() {
        return "ExportRecord{" +
                "exportRecType=" + exportRecType +
                ", exportTimestamp='" + exportTimestamp + '\'' +
                ", exportSequenceNum=" + exportSequenceNum +
                ", exportBranchId='" + exportBranchId + '\'' +
                ", exportRegionCode='" + exportRegionCode + '\'' +
                '}';
    }
}
