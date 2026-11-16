package com.carddemo.interest.record;

import static com.carddemo.interest.record.Fixed.pad;
import static com.carddemo.interest.record.Fixed.text;
import static com.carddemo.interest.record.Fixed.unsigned;
import static com.carddemo.interest.record.Fixed.zoned;

import java.math.BigDecimal;

/** CVTRA02Y, RECLN 50. */
public record DisclosureGroupRecord(String acctGroupId, String tranTypeCd, int tranCatCd, BigDecimal intRate,
                                    String filler) {

    public static final int LENGTH = 50;

    public static DisclosureGroupRecord parse(String line) {
        String r = pad(line, LENGTH);
        return new DisclosureGroupRecord(text(r, 0, 10), text(r, 10, 2), (int) unsigned(r, 12, 4), zoned(r, 16, 6, 2),
                text(r, 22, 28));
    }
}
