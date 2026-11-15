package com.carddemo.interest.load;

/**
 * One line of the reject list (strategy item TL3): the file, the record number counted from 1, the
 * field and why. Never a field value or a record image (standards S6.5).
 */
public record LoadReject(String file, int recordNumber, String field, String reason) {

    String line() {
        return file + "\t" + recordNumber + "\t" + field + "\t" + reason;
    }
}
