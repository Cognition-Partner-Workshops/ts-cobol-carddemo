package com.carddemo.interest.load;

/**
 * One line of the reject list (strategy TL3): the file, the record number from 1, the field and why.
 * It never holds a value of the record (standards S6.5).
 */
public record RejectedRecord(String file, int recordNumber, String field, String reason) {

    /** A reject that concerns the record as a whole (its length). */
    public static final String WHOLE_RECORD = "(record)";

    public String line() {
        return file + "\t" + recordNumber + "\t" + field + "\t" + reason;
    }
}
