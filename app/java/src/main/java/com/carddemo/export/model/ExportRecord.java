package com.carddemo.export.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Base export record class representing the common header structure from CVEXPORT.cpy.
 * This class encapsulates the 40-byte common header and the 460-byte payload area.
 * 
 * COBOL Structure (Total: 500 bytes):
 * - EXPORT-REC-TYPE: PIC X(1) - 1 byte discriminator
 * - EXPORT-TIMESTAMP: PIC X(26) - 26 bytes ISO-8601 timestamp
 * - EXPORT-SEQUENCE-NUM: PIC 9(9) COMP - 4 bytes binary
 * - EXPORT-BRANCH-ID: PIC X(4) - 4 bytes
 * - EXPORT-REGION-CODE: PIC X(5) - 5 bytes
 * - EXPORT-RECORD-DATA: PIC X(460) - 460 bytes payload
 */
public final class ExportRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static final int TOTAL_RECORD_SIZE = 500;
    public static final int HEADER_SIZE = 40;
    public static final int DATA_SIZE = 460;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    
    private final RecordType recordType;
    private final Instant timestamp;
    private final Long sequenceNumber;
    private final String branchId;
    private final String regionCode;
    private final ExportRecordData data;
    
    private ExportRecord(Builder builder) {
        this.recordType = Objects.requireNonNull(builder.recordType, "Record type is required");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "Timestamp is required");
        this.sequenceNumber = Objects.requireNonNull(builder.sequenceNumber, "Sequence number is required");
        this.branchId = validateBranchId(builder.branchId);
        this.regionCode = validateRegionCode(builder.regionCode);
        this.data = Objects.requireNonNull(builder.data, "Record data is required");
        
        validateRecordTypeMatchesData();
    }
    
    private String validateBranchId(String branchId) {
        Objects.requireNonNull(branchId, "Branch ID is required");
        if (branchId.length() > 4) {
            throw new IllegalArgumentException("Branch ID must not exceed 4 characters");
        }
        return branchId;
    }
    
    private String validateRegionCode(String regionCode) {
        Objects.requireNonNull(regionCode, "Region code is required");
        if (regionCode.length() > 5) {
            throw new IllegalArgumentException("Region code must not exceed 5 characters");
        }
        return regionCode;
    }
    
    private void validateRecordTypeMatchesData() {
        RecordType dataType = data.getRecordType();
        if (recordType != dataType) {
            throw new IllegalArgumentException(
                    "Record type " + recordType + " does not match data type " + dataType);
        }
    }
    
    public RecordType getRecordType() {
        return recordType;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public LocalDateTime getTimestampAsLocalDateTime() {
        return LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
    }
    
    public String getFormattedTimestamp() {
        return TIMESTAMP_FORMATTER.format(getTimestampAsLocalDateTime());
    }
    
    public Long getSequenceNumber() {
        return sequenceNumber;
    }
    
    public String getBranchId() {
        return branchId;
    }
    
    public String getRegionCode() {
        return regionCode;
    }
    
    public ExportRecordData getData() {
        return data;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends ExportRecordData> T getDataAs(Class<T> dataClass) {
        if (!dataClass.isInstance(data)) {
            throw new ClassCastException(
                    "Cannot cast " + data.getClass().getSimpleName() + " to " + dataClass.getSimpleName());
        }
        return (T) data;
    }
    
    public boolean isCustomerRecord() {
        return recordType == RecordType.CUSTOMER;
    }
    
    public boolean isAccountRecord() {
        return recordType == RecordType.ACCOUNT;
    }
    
    public boolean isCardXrefRecord() {
        return recordType == RecordType.CARD_XREF;
    }
    
    public boolean isTransactionRecord() {
        return recordType == RecordType.TRANSACTION;
    }
    
    public boolean isCardRecord() {
        return recordType == RecordType.CARD;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportRecord that = (ExportRecord) o;
        return recordType == that.recordType &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(sequenceNumber, that.sequenceNumber) &&
                Objects.equals(branchId, that.branchId) &&
                Objects.equals(regionCode, that.regionCode) &&
                Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordType, timestamp, sequenceNumber, branchId, regionCode, data);
    }
    
    @Override
    public String toString() {
        return "ExportRecord{" +
                "recordType=" + recordType +
                ", timestamp=" + getFormattedTimestamp() +
                ", sequenceNumber=" + sequenceNumber +
                ", branchId='" + branchId + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", data=" + data.getClass().getSimpleName() +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(ExportRecord source) {
        return new Builder()
                .recordType(source.recordType)
                .timestamp(source.timestamp)
                .sequenceNumber(source.sequenceNumber)
                .branchId(source.branchId)
                .regionCode(source.regionCode)
                .data(source.data);
    }
    
    public static final class Builder {
        private RecordType recordType;
        private Instant timestamp;
        private Long sequenceNumber;
        private String branchId;
        private String regionCode;
        private ExportRecordData data;
        
        private Builder() {}
        
        public Builder recordType(RecordType recordType) {
            this.recordType = recordType;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp.toInstant(ZoneOffset.UTC);
            return this;
        }
        
        public Builder timestampNow() {
            this.timestamp = Instant.now();
            return this;
        }
        
        public Builder sequenceNumber(Long sequenceNumber) {
            if (sequenceNumber != null && (sequenceNumber < 0 || sequenceNumber > 999999999L)) {
                throw new IllegalArgumentException(
                        "Sequence number must be between 0 and 999999999");
            }
            this.sequenceNumber = sequenceNumber;
            return this;
        }
        
        public Builder branchId(String branchId) {
            this.branchId = branchId;
            return this;
        }
        
        public Builder regionCode(String regionCode) {
            this.regionCode = regionCode;
            return this;
        }
        
        public Builder data(ExportRecordData data) {
            this.data = data;
            if (data != null) {
                this.recordType = data.getRecordType();
            }
            return this;
        }
        
        public ExportRecord build() {
            return new ExportRecord(this);
        }
    }
}
