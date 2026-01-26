package com.carddemo.export.serialization;

import com.carddemo.export.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Processor for batch operations on export records.
 * Provides filtering, transformation, and aggregation capabilities
 * for processing export files during branch migration.
 * 
 * Implements the Strategy pattern for type-specific processing logic.
 */
public final class ExportRecordProcessor {
    
    private final List<ExportRecord> records;
    
    public ExportRecordProcessor() {
        this.records = new ArrayList<>();
    }
    
    public ExportRecordProcessor(List<ExportRecord> records) {
        this.records = new ArrayList<>(records);
    }
    
    public void addRecord(ExportRecord record) {
        records.add(record);
    }
    
    public void addRecords(List<ExportRecord> newRecords) {
        records.addAll(newRecords);
    }
    
    public List<ExportRecord> getAllRecords() {
        return new ArrayList<>(records);
    }
    
    public int getRecordCount() {
        return records.size();
    }
    
    public List<ExportRecord> filterByType(RecordType recordType) {
        return records.stream()
                .filter(r -> r.getRecordType() == recordType)
                .collect(Collectors.toList());
    }
    
    public List<CustomerExportData> getCustomerRecords() {
        return records.stream()
                .filter(ExportRecord::isCustomerRecord)
                .map(r -> r.getDataAs(CustomerExportData.class))
                .collect(Collectors.toList());
    }
    
    public List<AccountExportData> getAccountRecords() {
        return records.stream()
                .filter(ExportRecord::isAccountRecord)
                .map(r -> r.getDataAs(AccountExportData.class))
                .collect(Collectors.toList());
    }
    
    public List<CardXrefExportData> getCardXrefRecords() {
        return records.stream()
                .filter(ExportRecord::isCardXrefRecord)
                .map(r -> r.getDataAs(CardXrefExportData.class))
                .collect(Collectors.toList());
    }
    
    public List<TransactionExportData> getTransactionRecords() {
        return records.stream()
                .filter(ExportRecord::isTransactionRecord)
                .map(r -> r.getDataAs(TransactionExportData.class))
                .collect(Collectors.toList());
    }
    
    public List<CardExportData> getCardRecords() {
        return records.stream()
                .filter(ExportRecord::isCardRecord)
                .map(r -> r.getDataAs(CardExportData.class))
                .collect(Collectors.toList());
    }
    
    public List<ExportRecord> filterByBranch(String branchId) {
        return records.stream()
                .filter(r -> branchId.equals(r.getBranchId()))
                .collect(Collectors.toList());
    }
    
    public List<ExportRecord> filterByRegion(String regionCode) {
        return records.stream()
                .filter(r -> regionCode.equals(r.getRegionCode()))
                .collect(Collectors.toList());
    }
    
    public List<ExportRecord> filter(Predicate<ExportRecord> predicate) {
        return records.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    public void forEach(Consumer<ExportRecord> action) {
        records.forEach(action);
    }
    
    public void forEachCustomer(Consumer<CustomerExportData> action) {
        records.stream()
                .filter(ExportRecord::isCustomerRecord)
                .map(r -> r.getDataAs(CustomerExportData.class))
                .forEach(action);
    }
    
    public void forEachAccount(Consumer<AccountExportData> action) {
        records.stream()
                .filter(ExportRecord::isAccountRecord)
                .map(r -> r.getDataAs(AccountExportData.class))
                .forEach(action);
    }
    
    public void forEachCardXref(Consumer<CardXrefExportData> action) {
        records.stream()
                .filter(ExportRecord::isCardXrefRecord)
                .map(r -> r.getDataAs(CardXrefExportData.class))
                .forEach(action);
    }
    
    public void forEachTransaction(Consumer<TransactionExportData> action) {
        records.stream()
                .filter(ExportRecord::isTransactionRecord)
                .map(r -> r.getDataAs(TransactionExportData.class))
                .forEach(action);
    }
    
    public void forEachCard(Consumer<CardExportData> action) {
        records.stream()
                .filter(ExportRecord::isCardRecord)
                .map(r -> r.getDataAs(CardExportData.class))
                .forEach(action);
    }
    
    public <T> T process(ExportRecordVisitor<T> visitor, ExportRecord record) {
        return ExportRecordVisitor.visit(record, visitor);
    }
    
    public <T> List<T> processAll(ExportRecordVisitor<T> visitor) {
        return records.stream()
                .map(r -> ExportRecordVisitor.visit(r, visitor))
                .collect(Collectors.toList());
    }
    
    public RecordTypeCounts getRecordTypeCounts() {
        long customerCount = records.stream().filter(ExportRecord::isCustomerRecord).count();
        long accountCount = records.stream().filter(ExportRecord::isAccountRecord).count();
        long cardXrefCount = records.stream().filter(ExportRecord::isCardXrefRecord).count();
        long transactionCount = records.stream().filter(ExportRecord::isTransactionRecord).count();
        long cardCount = records.stream().filter(ExportRecord::isCardRecord).count();
        
        return new RecordTypeCounts(customerCount, accountCount, cardXrefCount, transactionCount, cardCount);
    }
    
    public record RecordTypeCounts(
            long customerCount,
            long accountCount,
            long cardXrefCount,
            long transactionCount,
            long cardCount
    ) {
        public long total() {
            return customerCount + accountCount + cardXrefCount + transactionCount + cardCount;
        }
        
        @Override
        public String toString() {
            return String.format(
                    "RecordTypeCounts{customers=%d, accounts=%d, cardXrefs=%d, transactions=%d, cards=%d, total=%d}",
                    customerCount, accountCount, cardXrefCount, transactionCount, cardCount, total());
        }
    }
    
    public void clear() {
        records.clear();
    }
}
