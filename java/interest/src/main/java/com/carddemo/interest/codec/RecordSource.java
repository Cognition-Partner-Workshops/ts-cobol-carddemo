package com.carddemo.interest.codec;

/** Where a record is: the file name and the record number in it, counted from 1. Named in every rejection. */
public record RecordSource(String file, int recordNumber) {

    @Override
    public String toString() {
        return "file " + file + " record " + recordNumber;
    }
}
