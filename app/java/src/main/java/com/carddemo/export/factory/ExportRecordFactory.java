package com.carddemo.export.factory;

import com.carddemo.export.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory for creating ExportRecord instances with the appropriate data type.
 * Implements the Factory pattern for creating export records based on record type.
 * 
 * This factory provides convenience methods for creating complete export records
 * with proper header information and type-specific data payloads.
 */
public final class ExportRecordFactory {
    
    private final String defaultBranchId;
    private final String defaultRegionCode;
    private final AtomicLong sequenceGenerator;
    
    public ExportRecordFactory() {
        this("0000", "00000");
    }
    
    public ExportRecordFactory(String defaultBranchId, String defaultRegionCode) {
        this.defaultBranchId = defaultBranchId;
        this.defaultRegionCode = defaultRegionCode;
        this.sequenceGenerator = new AtomicLong(0);
    }
    
    public ExportRecordFactory(String defaultBranchId, String defaultRegionCode, long initialSequence) {
        this.defaultBranchId = defaultBranchId;
        this.defaultRegionCode = defaultRegionCode;
        this.sequenceGenerator = new AtomicLong(initialSequence);
    }
    
    public long getNextSequenceNumber() {
        return sequenceGenerator.incrementAndGet();
    }
    
    public void resetSequence() {
        sequenceGenerator.set(0);
    }
    
    public void setSequence(long value) {
        sequenceGenerator.set(value);
    }
    
    public ExportRecord createCustomerRecord(CustomerExportData customerData) {
        return createRecord(customerData);
    }
    
    public ExportRecord createCustomerRecord(CustomerExportData customerData, 
                                              String branchId, String regionCode) {
        return createRecord(customerData, branchId, regionCode);
    }
    
    public ExportRecord createAccountRecord(AccountExportData accountData) {
        return createRecord(accountData);
    }
    
    public ExportRecord createAccountRecord(AccountExportData accountData,
                                             String branchId, String regionCode) {
        return createRecord(accountData, branchId, regionCode);
    }
    
    public ExportRecord createCardXrefRecord(CardXrefExportData cardXrefData) {
        return createRecord(cardXrefData);
    }
    
    public ExportRecord createCardXrefRecord(CardXrefExportData cardXrefData,
                                              String branchId, String regionCode) {
        return createRecord(cardXrefData, branchId, regionCode);
    }
    
    public ExportRecord createTransactionRecord(TransactionExportData transactionData) {
        return createRecord(transactionData);
    }
    
    public ExportRecord createTransactionRecord(TransactionExportData transactionData,
                                                 String branchId, String regionCode) {
        return createRecord(transactionData, branchId, regionCode);
    }
    
    public ExportRecord createCardRecord(CardExportData cardData) {
        return createRecord(cardData);
    }
    
    public ExportRecord createCardRecord(CardExportData cardData,
                                          String branchId, String regionCode) {
        return createRecord(cardData, branchId, regionCode);
    }
    
    public ExportRecord createRecord(ExportRecordData data) {
        return createRecord(data, defaultBranchId, defaultRegionCode);
    }
    
    public ExportRecord createRecord(ExportRecordData data, String branchId, String regionCode) {
        return ExportRecord.builder()
                .data(data)
                .timestampNow()
                .sequenceNumber(getNextSequenceNumber())
                .branchId(branchId)
                .regionCode(regionCode)
                .build();
    }
    
    public ExportRecord createRecord(ExportRecordData data, Instant timestamp,
                                      Long sequenceNumber, String branchId, String regionCode) {
        return ExportRecord.builder()
                .data(data)
                .timestamp(timestamp)
                .sequenceNumber(sequenceNumber)
                .branchId(branchId)
                .regionCode(regionCode)
                .build();
    }
    
    public ExportRecord createRecord(ExportRecordData data, LocalDateTime timestamp,
                                      Long sequenceNumber, String branchId, String regionCode) {
        return ExportRecord.builder()
                .data(data)
                .timestamp(timestamp)
                .sequenceNumber(sequenceNumber)
                .branchId(branchId)
                .regionCode(regionCode)
                .build();
    }
    
    public static ExportRecordData createEmptyData(RecordType recordType) {
        return switch (recordType) {
            case CUSTOMER -> CustomerExportData.builder()
                    .customerId(0L)
                    .build();
            case ACCOUNT -> AccountExportData.builder()
                    .accountId("0")
                    .build();
            case CARD_XREF -> CardXrefExportData.builder()
                    .cardNumber("")
                    .customerId("0")
                    .accountId(0L)
                    .build();
            case TRANSACTION -> TransactionExportData.builder()
                    .transactionId("")
                    .build();
            case CARD -> CardExportData.builder()
                    .cardNumber("")
                    .accountId(0L)
                    .build();
        };
    }
    
    public static Class<? extends ExportRecordData> getDataClass(RecordType recordType) {
        return switch (recordType) {
            case CUSTOMER -> CustomerExportData.class;
            case ACCOUNT -> AccountExportData.class;
            case CARD_XREF -> CardXrefExportData.class;
            case TRANSACTION -> TransactionExportData.class;
            case CARD -> CardExportData.class;
        };
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private String branchId = "0000";
        private String regionCode = "00000";
        private long initialSequence = 0;
        
        private Builder() {}
        
        public Builder branchId(String branchId) {
            this.branchId = branchId;
            return this;
        }
        
        public Builder regionCode(String regionCode) {
            this.regionCode = regionCode;
            return this;
        }
        
        public Builder initialSequence(long initialSequence) {
            this.initialSequence = initialSequence;
            return this;
        }
        
        public ExportRecordFactory build() {
            return new ExportRecordFactory(branchId, regionCode, initialSequence);
        }
    }
}
