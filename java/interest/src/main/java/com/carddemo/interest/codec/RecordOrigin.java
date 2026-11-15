package com.carddemo.interest.codec;

/** Where a record comes from or goes to: the file and the record's number in it, from 1. Names an error. */
public record RecordOrigin(String file, int recordNumber) {

    /** A record built in memory and not yet part of a file. */
    public static final RecordOrigin IN_MEMORY = new RecordOrigin("(in memory)", 0);

    @Override
    public String toString() {
        return "file " + file + " record " + recordNumber;
    }
}
