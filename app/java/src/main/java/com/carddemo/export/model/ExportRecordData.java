package com.carddemo.export.model;

import java.io.Serializable;

/**
 * Marker interface for all export record data types.
 * This interface is implemented by all five data classes that correspond
 * to the COBOL EXPORT-RECORD-DATA REDEFINES structure.
 * 
 * Implementing classes:
 * - CustomerExportData (Type 'C')
 * - AccountExportData (Type 'A')
 * - CardXrefExportData (Type 'X')
 * - TransactionExportData (Type 'T')
 * - CardExportData (Type 'D')
 */
public sealed interface ExportRecordData extends Serializable
        permits CustomerExportData, AccountExportData, CardXrefExportData, 
                TransactionExportData, CardExportData {
    
    /**
     * Returns the record type associated with this data.
     */
    RecordType getRecordType();
    
    /**
     * Returns the size of this record data in bytes when serialized to fixed-length format.
     * All implementations must return 460 to match the COBOL EXPORT-RECORD-DATA size.
     */
    default int getDataSize() {
        return 460;
    }
}
