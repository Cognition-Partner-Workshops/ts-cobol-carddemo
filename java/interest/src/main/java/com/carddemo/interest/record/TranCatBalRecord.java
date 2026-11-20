package com.carddemo.interest.record;

import static com.carddemo.interest.record.Fixed.pad;
import static com.carddemo.interest.record.Fixed.text;
import static com.carddemo.interest.record.Fixed.unsigned;
import static com.carddemo.interest.record.Fixed.zoned;

import java.math.BigDecimal;

/** CVTRA01Y, RECLN 50. */
public record TranCatBalRecord(long acctId, String tranTypeCd, int tranCatCd, BigDecimal tranCatBal, String filler,
                               String rawRecord) {

    public static final int LENGTH = 50;

    public static TranCatBalRecord parse(String line) {
        String r = pad(line, LENGTH);
        return new TranCatBalRecord(unsigned(r, 0, 11), text(r, 11, 2), (int) unsigned(r, 13, 4), zoned(r, 17, 11, 2),
                text(r, 28, 22), r);
    }

    public String format() {
        if (rawRecord != null) {
            return rawRecord;
        }
        return unsigned(acctId, 11) + pad(tranTypeCd, 2) + unsigned(tranCatCd, 4) + zoned(tranCatBal, 11, 2)
                + pad(filler, 22);
    }
}
