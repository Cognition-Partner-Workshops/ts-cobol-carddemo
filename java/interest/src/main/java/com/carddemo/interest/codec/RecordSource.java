package com.carddemo.interest.codec;

/** Where a record came from: the file name and the record's number in it, counted from 1. Used in every codec error. */
public record RecordSource(String file, long recordNumber) {

    /** For a record that is built by the run and not read from a file. */
    public static RecordSource built(String file) {
        return new RecordSource(file, 0);
    }

    String describe() {
        return recordNumber > 0 ? "file " + file + ", record " + recordNumber : "file " + file + ", new record";
    }
}
