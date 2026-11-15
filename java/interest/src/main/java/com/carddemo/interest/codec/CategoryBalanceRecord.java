package com.carddemo.interest.codec;

import com.carddemo.interest.domain.CategoryBalance;

/** Layout CVTRA01Y (app/cpy/CVTRA01Y.cpy:4-10, RECLN 50). Data-map rows CVTRA01Y/*. */
public final class CategoryBalanceRecord {

    public static final RecordLayout LAYOUT = RecordLayout.of("CVTRA01Y")
            .unsigned("TRANCAT-ACCT-ID", 11)
            .text("TRANCAT-TYPE-CD", 2)
            .unsigned("TRANCAT-CD", 4)
            .signed("TRAN-CAT-BAL", 9, 2)
            .filler(22)
            .build();
    public static final int LENGTH = LAYOUT.length();

    private static final RecordCodec FIXTURE = new RecordCodec(LAYOUT, CodePage.fixture());
    /** TRAN-CAT-KEY: the three key fields, 17 bytes. */
    private static final int KEY_LENGTH = LAYOUT.field("TRAN-CAT-BAL").offset();

    private CategoryBalanceRecord() {
    }

    public static CategoryBalance from(RecordValues values) {
        return new CategoryBalance(
                values.unsigned("TRANCAT-ACCT-ID"),
                values.text("TRANCAT-TYPE-CD"),
                (int) values.unsigned("TRANCAT-CD"),
                values.signed("TRAN-CAT-BAL"));
    }

    /** Decodes a record image of the fixture code page. */
    public static CategoryBalance decode(String image) {
        return decode(image, RecordOrigin.IN_MEMORY);
    }

    public static CategoryBalance decode(String image, RecordOrigin origin) {
        return from(FIXTURE.decode(FixtureCodePage.bytes(image), origin));
    }

    public static String key(String image) {
        return image.substring(0, KEY_LENGTH);
    }
}
