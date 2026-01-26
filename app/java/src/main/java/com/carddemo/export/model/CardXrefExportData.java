package com.carddemo.export.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Card cross-reference export data corresponding to COBOL EXPORT-CARD-XREF-DATA structure.
 * Maps to record type 'X' in the CVEXPORT multi-record format.
 * 
 * This structure links cards to customers and accounts, enabling lookups
 * across the three master files during branch migration.
 * 
 * COBOL Structure (460 bytes total):
 * - EXP-XREF-CARD-NUM: PIC X(16) - 16 bytes
 * - EXP-XREF-CUST-ID: PIC 9(09) - 9 bytes display
 * - EXP-XREF-ACCT-ID: PIC 9(11) COMP - 8 bytes binary (Long in Java)
 * - FILLER: PIC X(427) - 427 bytes
 */
public final class CardXrefExportData implements ExportRecordData {
    
    private static final long serialVersionUID = 1L;
    
    private final String cardNumber;
    private final String customerId;
    private final Long accountId;
    
    private CardXrefExportData(Builder builder) {
        this.cardNumber = validateCardNumber(builder.cardNumber);
        this.customerId = validateCustomerId(builder.customerId);
        this.accountId = Objects.requireNonNull(builder.accountId, "Account ID is required");
    }
    
    private String validateCardNumber(String cardNumber) {
        Objects.requireNonNull(cardNumber, "Card number is required");
        if (cardNumber.length() > 16) {
            return cardNumber.substring(0, 16);
        }
        return cardNumber;
    }
    
    private String validateCustomerId(String customerId) {
        Objects.requireNonNull(customerId, "Customer ID is required");
        String digitsOnly = customerId.replaceAll("[^0-9]", "");
        if (digitsOnly.length() > 9) {
            return digitsOnly.substring(0, 9);
        }
        return digitsOnly;
    }
    
    @Override
    public RecordType getRecordType() {
        return RecordType.CARD_XREF;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****-****-****-****";
        }
        int len = cardNumber.length();
        return "****-****-****-" + cardNumber.substring(len - 4);
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardXrefExportData that = (CardXrefExportData) o;
        return Objects.equals(cardNumber, that.cardNumber) &&
                Objects.equals(customerId, that.customerId) &&
                Objects.equals(accountId, that.accountId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, customerId, accountId);
    }
    
    @Override
    public String toString() {
        return "CardXrefExportData{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", customerId='" + customerId + '\'' +
                ", accountId=" + accountId +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(CardXrefExportData source) {
        return new Builder()
                .cardNumber(source.cardNumber)
                .customerId(source.customerId)
                .accountId(source.accountId);
    }
    
    public static final class Builder {
        private String cardNumber;
        private String customerId;
        private Long accountId;
        
        private Builder() {}
        
        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            return this;
        }
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder customerId(Long customerId) {
            this.customerId = customerId != null ? String.valueOf(customerId) : null;
            return this;
        }
        
        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }
        
        public Builder accountId(String accountId) {
            if (accountId != null && !accountId.isBlank()) {
                this.accountId = Long.parseLong(accountId.replaceAll("[^0-9]", ""));
            }
            return this;
        }
        
        public CardXrefExportData build() {
            return new CardXrefExportData(this);
        }
    }
}
