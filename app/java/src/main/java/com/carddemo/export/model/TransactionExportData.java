package com.carddemo.export.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Transaction export data corresponding to COBOL EXPORT-TRANSACTION-DATA structure.
 * Maps to record type 'T' in the CVEXPORT multi-record format.
 * 
 * COBOL Structure (460 bytes total):
 * - EXP-TRAN-ID: PIC X(16) - 16 bytes
 * - EXP-TRAN-TYPE-CD: PIC X(02) - 2 bytes
 * - EXP-TRAN-CAT-CD: PIC 9(04) - 4 bytes display
 * - EXP-TRAN-SOURCE: PIC X(10) - 10 bytes
 * - EXP-TRAN-DESC: PIC X(100) - 100 bytes
 * - EXP-TRAN-AMT: PIC S9(09)V99 COMP-3 - 6 bytes packed decimal (BigDecimal in Java)
 * - EXP-TRAN-MERCHANT-ID: PIC 9(09) COMP - 4 bytes binary (Long in Java)
 * - EXP-TRAN-MERCHANT-NAME: PIC X(50) - 50 bytes
 * - EXP-TRAN-MERCHANT-CITY: PIC X(50) - 50 bytes
 * - EXP-TRAN-MERCHANT-ZIP: PIC X(10) - 10 bytes
 * - EXP-TRAN-CARD-NUM: PIC X(16) - 16 bytes
 * - EXP-TRAN-ORIG-TS: PIC X(26) - 26 bytes ISO-8601 timestamp
 * - EXP-TRAN-PROC-TS: PIC X(26) - 26 bytes ISO-8601 timestamp
 * - FILLER: PIC X(140) - 140 bytes
 */
public final class TransactionExportData implements ExportRecordData {
    
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    private static final int MONETARY_SCALE = 2;
    
    private final String transactionId;
    private final String transactionTypeCode;
    private final Integer transactionCategoryCode;
    private final String transactionSource;
    private final String description;
    private final BigDecimal amount;
    private final Long merchantId;
    private final String merchantName;
    private final String merchantCity;
    private final String merchantZipCode;
    private final String cardNumber;
    private final Instant originationTimestamp;
    private final Instant processingTimestamp;
    
    private TransactionExportData(Builder builder) {
        this.transactionId = validateTransactionId(builder.transactionId);
        this.transactionTypeCode = truncateOrPad(builder.transactionTypeCode, 2);
        this.transactionCategoryCode = validateCategoryCode(builder.transactionCategoryCode);
        this.transactionSource = truncateOrPad(builder.transactionSource, 10);
        this.description = truncateOrPad(builder.description, 100);
        this.amount = normalizeMonetary(builder.amount);
        this.merchantId = builder.merchantId;
        this.merchantName = truncateOrPad(builder.merchantName, 50);
        this.merchantCity = truncateOrPad(builder.merchantCity, 50);
        this.merchantZipCode = truncateOrPad(builder.merchantZipCode, 10);
        this.cardNumber = truncateOrPad(builder.cardNumber, 16);
        this.originationTimestamp = builder.originationTimestamp;
        this.processingTimestamp = builder.processingTimestamp;
    }
    
    private String validateTransactionId(String transactionId) {
        Objects.requireNonNull(transactionId, "Transaction ID is required");
        if (transactionId.length() > 16) {
            return transactionId.substring(0, 16);
        }
        return transactionId;
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
    
    private Integer validateCategoryCode(Integer code) {
        if (code == null) {
            return null;
        }
        if (code < 0 || code > 9999) {
            throw new IllegalArgumentException("Transaction category code must be between 0 and 9999");
        }
        return code;
    }
    
    private BigDecimal normalizeMonetary(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }
    
    @Override
    public RecordType getRecordType() {
        return RecordType.TRANSACTION;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }
    
    public Integer getTransactionCategoryCode() {
        return transactionCategoryCode;
    }
    
    public String getTransactionSource() {
        return transactionSource;
    }
    
    public String getDescription() {
        return description;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public boolean isCredit() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isDebit() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    public BigDecimal getAbsoluteAmount() {
        return amount.abs();
    }
    
    public Long getMerchantId() {
        return merchantId;
    }
    
    public String getMerchantName() {
        return merchantName;
    }
    
    public String getMerchantCity() {
        return merchantCity;
    }
    
    public String getMerchantZipCode() {
        return merchantZipCode;
    }
    
    public String getMerchantLocation() {
        StringBuilder sb = new StringBuilder();
        if (!merchantCity.isBlank()) {
            sb.append(merchantCity.trim());
        }
        if (!merchantZipCode.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(merchantZipCode.trim());
        }
        return sb.toString();
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
    
    public Instant getOriginationTimestamp() {
        return originationTimestamp;
    }
    
    public LocalDateTime getOriginationTimestampAsLocalDateTime() {
        return originationTimestamp != null 
                ? LocalDateTime.ofInstant(originationTimestamp, ZoneOffset.UTC) 
                : null;
    }
    
    public String getFormattedOriginationTimestamp() {
        LocalDateTime ldt = getOriginationTimestampAsLocalDateTime();
        return ldt != null ? TIMESTAMP_FORMATTER.format(ldt) : "";
    }
    
    public Instant getProcessingTimestamp() {
        return processingTimestamp;
    }
    
    public LocalDateTime getProcessingTimestampAsLocalDateTime() {
        return processingTimestamp != null 
                ? LocalDateTime.ofInstant(processingTimestamp, ZoneOffset.UTC) 
                : null;
    }
    
    public String getFormattedProcessingTimestamp() {
        LocalDateTime ldt = getProcessingTimestampAsLocalDateTime();
        return ldt != null ? TIMESTAMP_FORMATTER.format(ldt) : "";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionExportData that = (TransactionExportData) o;
        return Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(transactionTypeCode, that.transactionTypeCode) &&
                Objects.equals(transactionCategoryCode, that.transactionCategoryCode) &&
                Objects.equals(transactionSource, that.transactionSource) &&
                Objects.equals(description, that.description) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(merchantId, that.merchantId) &&
                Objects.equals(merchantName, that.merchantName) &&
                Objects.equals(merchantCity, that.merchantCity) &&
                Objects.equals(merchantZipCode, that.merchantZipCode) &&
                Objects.equals(cardNumber, that.cardNumber) &&
                Objects.equals(originationTimestamp, that.originationTimestamp) &&
                Objects.equals(processingTimestamp, that.processingTimestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId, transactionTypeCode, transactionCategoryCode,
                transactionSource, description, amount, merchantId, merchantName,
                merchantCity, merchantZipCode, cardNumber, originationTimestamp, processingTimestamp);
    }
    
    @Override
    public String toString() {
        return "TransactionExportData{" +
                "transactionId='" + transactionId + '\'' +
                ", typeCode='" + transactionTypeCode + '\'' +
                ", categoryCode=" + transactionCategoryCode +
                ", amount=" + amount +
                ", merchantName='" + merchantName + '\'' +
                ", cardNumber='" + getMaskedCardNumber() + '\'' +
                ", originationTimestamp=" + getFormattedOriginationTimestamp() +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(TransactionExportData source) {
        return new Builder()
                .transactionId(source.transactionId)
                .transactionTypeCode(source.transactionTypeCode)
                .transactionCategoryCode(source.transactionCategoryCode)
                .transactionSource(source.transactionSource)
                .description(source.description)
                .amount(source.amount)
                .merchantId(source.merchantId)
                .merchantName(source.merchantName)
                .merchantCity(source.merchantCity)
                .merchantZipCode(source.merchantZipCode)
                .cardNumber(source.cardNumber)
                .originationTimestamp(source.originationTimestamp)
                .processingTimestamp(source.processingTimestamp);
    }
    
    public static final class Builder {
        private String transactionId;
        private String transactionTypeCode;
        private Integer transactionCategoryCode;
        private String transactionSource;
        private String description;
        private BigDecimal amount;
        private Long merchantId;
        private String merchantName;
        private String merchantCity;
        private String merchantZipCode;
        private String cardNumber;
        private Instant originationTimestamp;
        private Instant processingTimestamp;
        
        private Builder() {}
        
        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        
        public Builder transactionTypeCode(String transactionTypeCode) {
            this.transactionTypeCode = transactionTypeCode;
            return this;
        }
        
        public Builder transactionCategoryCode(Integer transactionCategoryCode) {
            this.transactionCategoryCode = transactionCategoryCode;
            return this;
        }
        
        public Builder transactionCategoryCode(String transactionCategoryCode) {
            if (transactionCategoryCode != null && !transactionCategoryCode.isBlank()) {
                this.transactionCategoryCode = Integer.parseInt(transactionCategoryCode.trim());
            }
            return this;
        }
        
        public Builder transactionSource(String transactionSource) {
            this.transactionSource = transactionSource;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder amount(String amount) {
            if (amount != null && !amount.isBlank()) {
                this.amount = new BigDecimal(amount);
            }
            return this;
        }
        
        public Builder merchantId(Long merchantId) {
            this.merchantId = merchantId;
            return this;
        }
        
        public Builder merchantId(String merchantId) {
            if (merchantId != null && !merchantId.isBlank()) {
                this.merchantId = Long.parseLong(merchantId.replaceAll("[^0-9]", ""));
            }
            return this;
        }
        
        public Builder merchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }
        
        public Builder merchantCity(String merchantCity) {
            this.merchantCity = merchantCity;
            return this;
        }
        
        public Builder merchantZipCode(String merchantZipCode) {
            this.merchantZipCode = merchantZipCode;
            return this;
        }
        
        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            return this;
        }
        
        public Builder originationTimestamp(Instant originationTimestamp) {
            this.originationTimestamp = originationTimestamp;
            return this;
        }
        
        public Builder originationTimestamp(LocalDateTime originationTimestamp) {
            this.originationTimestamp = originationTimestamp != null 
                    ? originationTimestamp.toInstant(ZoneOffset.UTC) 
                    : null;
            return this;
        }
        
        public Builder processingTimestamp(Instant processingTimestamp) {
            this.processingTimestamp = processingTimestamp;
            return this;
        }
        
        public Builder processingTimestamp(LocalDateTime processingTimestamp) {
            this.processingTimestamp = processingTimestamp != null 
                    ? processingTimestamp.toInstant(ZoneOffset.UTC) 
                    : null;
            return this;
        }
        
        public Builder processingTimestampNow() {
            this.processingTimestamp = Instant.now();
            return this;
        }
        
        public TransactionExportData build() {
            return new TransactionExportData(this);
        }
    }
}
