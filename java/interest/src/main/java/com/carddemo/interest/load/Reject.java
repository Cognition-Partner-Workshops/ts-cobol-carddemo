package com.carddemo.interest.load;

/**
 * One line of the reject list (strategy item TL3): the file, the record's number in it counted
 * from 1, the copybook field and the rule that was broken. It holds no value of the record.
 */
public record Reject(String file, long recordNumber, String field, String reason) {

    String line() {
        return file + "\t" + recordNumber + "\t" + field + "\t" + reason;
    }
}
