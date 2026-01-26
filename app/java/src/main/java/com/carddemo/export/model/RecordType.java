package com.carddemo.export.model;

/**
 * Enumeration representing the five record types in the CVEXPORT multi-record structure.
 * Maps to the COBOL EXPORT-REC-TYPE field (PIC X(1)).
 * 
 * Record type discriminators:
 * - 'C' = Customer master data
 * - 'A' = Account master data
 * - 'X' = Card cross-reference
 * - 'T' = Transaction history
 * - 'D' = Card master data
 */
public enum RecordType {
    CUSTOMER('C'),
    ACCOUNT('A'),
    CARD_XREF('X'),
    TRANSACTION('T'),
    CARD('D');

    private final char code;

    RecordType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static RecordType fromCode(char code) {
        for (RecordType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown record type code: " + code);
    }

    public static RecordType fromCode(String code) {
        if (code == null || code.length() != 1) {
            throw new IllegalArgumentException("Record type code must be a single character");
        }
        return fromCode(code.charAt(0));
    }
}
