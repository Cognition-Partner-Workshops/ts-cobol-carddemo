package com.carddemo.interest.codec;

import java.math.BigDecimal;

/** Layout CVTRA02Y (app/cpy/CVTRA02Y.cpy:4-10, RECLN 50). Data-map rows CVTRA02Y/*. */
public final class DisclosureGroupRecord {

    public static final RecordLayout LAYOUT = RecordLayout.of("CVTRA02Y")
            .text("DIS-ACCT-GROUP-ID", 10)
            .text("DIS-TRAN-TYPE-CD", 2)
            .unsigned("DIS-TRAN-CAT-CD", 4)
            .signed("DIS-INT-RATE", 4, 2)
            .filler(28)
            .build();
    public static final int LENGTH = LAYOUT.length();

    private static final RecordCodec FIXTURE = new RecordCodec(LAYOUT, CodePage.fixture());

    private DisclosureGroupRecord() {
    }

    /** One rate of a disclosure group: yearly rate in percent, (6,2). */
    public record Rate(String groupId, String typeCode, int categoryCode, BigDecimal yearlyRate) {
    }

    public static Rate from(RecordValues values) {
        return new Rate(
                values.text("DIS-ACCT-GROUP-ID"),
                values.text("DIS-TRAN-TYPE-CD"),
                (int) values.unsigned("DIS-TRAN-CAT-CD"),
                values.signed("DIS-INT-RATE"));
    }

    /** Decodes a record image of the fixture code page. */
    public static Rate decode(String image) {
        return decode(image, RecordOrigin.IN_MEMORY);
    }

    public static Rate decode(String image, RecordOrigin origin) {
        return from(FIXTURE.decode(FixtureCodePage.bytes(image), origin));
    }
}
