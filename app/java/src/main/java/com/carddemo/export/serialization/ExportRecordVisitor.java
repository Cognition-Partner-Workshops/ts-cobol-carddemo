package com.carddemo.export.serialization;

import com.carddemo.export.model.*;

/**
 * Visitor interface for type-specific processing of export record data.
 * Implements the Visitor pattern to enable polymorphic operations on
 * different record types without modifying the data classes.
 * 
 * Usage example:
 * <pre>
 * ExportRecordVisitor<String> printer = new ExportRecordVisitor<>() {
 *     public String visitCustomer(CustomerExportData data) {
 *         return "Customer: " + data.getFullName();
 *     }
 *     // ... other visit methods
 * };
 * 
 * String result = ExportRecordVisitor.visit(record.getData(), printer);
 * </pre>
 * 
 * @param <T> The return type of the visitor operations
 */
public interface ExportRecordVisitor<T> {
    
    T visitCustomer(CustomerExportData customerData);
    
    T visitAccount(AccountExportData accountData);
    
    T visitCardXref(CardXrefExportData cardXrefData);
    
    T visitTransaction(TransactionExportData transactionData);
    
    T visitCard(CardExportData cardData);
    
    static <T> T visit(ExportRecordData data, ExportRecordVisitor<T> visitor) {
        return switch (data) {
            case CustomerExportData customer -> visitor.visitCustomer(customer);
            case AccountExportData account -> visitor.visitAccount(account);
            case CardXrefExportData cardXref -> visitor.visitCardXref(cardXref);
            case TransactionExportData transaction -> visitor.visitTransaction(transaction);
            case CardExportData card -> visitor.visitCard(card);
        };
    }
    
    static <T> T visit(ExportRecord record, ExportRecordVisitor<T> visitor) {
        return visit(record.getData(), visitor);
    }
}
