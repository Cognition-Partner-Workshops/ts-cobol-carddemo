package com.carddemo.interest.record;

import static com.carddemo.interest.record.Fixed.pad;
import static com.carddemo.interest.record.Fixed.text;
import static com.carddemo.interest.record.Fixed.unsigned;

/** CVACT03Y, RECLN 50. */
public record CardXrefRecord(String cardNum, long custId, long acctId, String filler) {

    public static final int LENGTH = 50;

    public static CardXrefRecord parse(String line) {
        String r = pad(line, LENGTH);
        return new CardXrefRecord(text(r, 0, 16), unsigned(r, 16, 9), unsigned(r, 25, 11), text(r, 36, 14));
    }
}
