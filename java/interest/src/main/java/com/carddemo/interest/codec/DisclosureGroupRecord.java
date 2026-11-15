package com.carddemo.interest.codec;

import java.math.BigDecimal;

/** Layout CVTRA02Y (app/cpy/CVTRA02Y.cpy:4-10, RECLN 50). Data-map rows CVTRA02Y/*. */
public final class DisclosureGroupRecord {

    public static final RecordLayout LAYOUT = CardDemoLayouts.DISCLOSURE_GROUP;
    public static final int LENGTH = LAYOUT.length();

    private DisclosureGroupRecord() {
    }

    /** One rate of a disclosure group: yearly rate in percent, (6,2). */
    public record Rate(String groupId, String typeCode, int categoryCode, BigDecimal yearlyRate) {
    }

    public static Rate decode(String image, RecordSource source) {
        DecodedRecord fields = LAYOUT.decode(RecordImage.bytes(image), CodePage.FIXTURE, source);
        return new Rate(
                fields.text("DIS-ACCT-GROUP-ID"),
                fields.text("DIS-TRAN-TYPE-CD"),
                fields.number("DIS-TRAN-CAT-CD").intValueExact(),
                fields.number("DIS-INT-RATE"));
    }
}
