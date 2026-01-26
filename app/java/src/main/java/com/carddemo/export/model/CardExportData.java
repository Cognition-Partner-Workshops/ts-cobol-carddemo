package com.carddemo.export.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Card export data corresponding to COBOL EXPORT-CARD-DATA structure.
 * Maps to record type 'D' in the CVEXPORT multi-record format.
 * 
 * COBOL Structure (460 bytes total):
 * - EXP-CARD-NUM: PIC X(16) - 16 bytes
 * - EXP-CARD-ACCT-ID: PIC 9(11) COMP - 8 bytes binary (Long in Java)
 * - EXP-CARD-CVV-CD: PIC 9(03) COMP - 2 bytes binary (Integer in Java)
 * - EXP-CARD-EMBOSSED-NAME: PIC X(50) - 50 bytes
 * - EXP-CARD-EXPIRAION-DATE: PIC X(10) - 10 bytes
 * - EXP-CARD-ACTIVE-STATUS: PIC X(01) - 1 byte
 * - FILLER: PIC X(373) - 373 bytes
 */
public final class CardExportData implements ExportRecordData {
    
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private final String cardNumber;
    private final Long accountId;
    private final Integer cvvCode;
    private final String embossedName;
    private final LocalDate expirationDate;
    private final String activeStatus;
    
    private CardExportData(Builder builder) {
        this.cardNumber = validateCardNumber(builder.cardNumber);
        this.accountId = Objects.requireNonNull(builder.accountId, "Account ID is required");
        this.cvvCode = validateCvvCode(builder.cvvCode);
        this.embossedName = truncateOrPad(builder.embossedName, 50);
        this.expirationDate = builder.expirationDate;
        this.activeStatus = truncateOrPad(builder.activeStatus, 1);
    }
    
    private String validateCardNumber(String cardNumber) {
        Objects.requireNonNull(cardNumber, "Card number is required");
        if (cardNumber.length() > 16) {
            return cardNumber.substring(0, 16);
        }
        return cardNumber;
    }
    
    private Integer validateCvvCode(Integer cvvCode) {
        if (cvvCode == null) {
            return null;
        }
        if (cvvCode < 0 || cvvCode > 999) {
            throw new IllegalArgumentException("CVV code must be between 0 and 999");
        }
        return cvvCode;
    }
    
    private String truncateOrPad(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }
    
    @Override
    public RecordType getRecordType() {
        return RecordType.CARD;
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
    
    public String getFormattedCardNumber() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "-" +
               cardNumber.substring(4, 8) + "-" +
               cardNumber.substring(8, 12) + "-" +
               cardNumber.substring(12, 16);
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public Integer getCvvCode() {
        return cvvCode;
    }
    
    public String getMaskedCvvCode() {
        return "***";
    }
    
    public String getEmbossedName() {
        return embossedName;
    }
    
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    public String getFormattedExpirationDate() {
        return expirationDate != null ? DATE_FORMATTER.format(expirationDate) : "";
    }
    
    public String getExpirationMonthYear() {
        if (expirationDate == null) {
            return "";
        }
        return String.format("%02d/%02d", 
                expirationDate.getMonthValue(), 
                expirationDate.getYear() % 100);
    }
    
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.isBefore(LocalDate.now());
    }
    
    public String getActiveStatus() {
        return activeStatus;
    }
    
    public boolean isActive() {
        return "Y".equalsIgnoreCase(activeStatus);
    }
    
    public boolean isUsable() {
        return isActive() && !isExpired();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardExportData that = (CardExportData) o;
        return Objects.equals(cardNumber, that.cardNumber) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(cvvCode, that.cvvCode) &&
                Objects.equals(embossedName, that.embossedName) &&
                Objects.equals(expirationDate, that.expirationDate) &&
                Objects.equals(activeStatus, that.activeStatus);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, accountId, cvvCode, embossedName, expirationDate, activeStatus);
    }
    
    @Override
    public String toString() {
        return "CardExportData{" +
                "cardNumber='" + getMaskedCardNumber() + '\'' +
                ", accountId=" + accountId +
                ", embossedName='" + embossedName + '\'' +
                ", expirationDate=" + getFormattedExpirationDate() +
                ", activeStatus='" + activeStatus + '\'' +
                ", isUsable=" + isUsable() +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(CardExportData source) {
        return new Builder()
                .cardNumber(source.cardNumber)
                .accountId(source.accountId)
                .cvvCode(source.cvvCode)
                .embossedName(source.embossedName)
                .expirationDate(source.expirationDate)
                .activeStatus(source.activeStatus);
    }
    
    public static final class Builder {
        private String cardNumber;
        private Long accountId;
        private Integer cvvCode;
        private String embossedName;
        private LocalDate expirationDate;
        private String activeStatus;
        
        private Builder() {}
        
        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
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
        
        public Builder cvvCode(Integer cvvCode) {
            this.cvvCode = cvvCode;
            return this;
        }
        
        public Builder cvvCode(String cvvCode) {
            if (cvvCode != null && !cvvCode.isBlank()) {
                this.cvvCode = Integer.parseInt(cvvCode.trim());
            }
            return this;
        }
        
        public Builder embossedName(String embossedName) {
            this.embossedName = embossedName;
            return this;
        }
        
        public Builder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }
        
        public Builder expirationDate(String expirationDate) {
            if (expirationDate != null && !expirationDate.isBlank()) {
                this.expirationDate = LocalDate.parse(expirationDate, DATE_FORMATTER);
            }
            return this;
        }
        
        public Builder activeStatus(String activeStatus) {
            this.activeStatus = activeStatus;
            return this;
        }
        
        public Builder active(boolean isActive) {
            this.activeStatus = isActive ? "Y" : "N";
            return this;
        }
        
        public CardExportData build() {
            return new CardExportData(this);
        }
    }
}
