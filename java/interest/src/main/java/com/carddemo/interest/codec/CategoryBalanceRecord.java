package com.carddemo.interest.codec;

import com.carddemo.interest.domain.CategoryBalance;

/** Layout CVTRA01Y (app/cpy/CVTRA01Y.cpy:4-10, RECLN 50). Data-map rows CVTRA01Y/*. */
public final class CategoryBalanceRecord {

    public static final RecordLayout LAYOUT = CardDemoLayouts.CATEGORY_BALANCE;
    public static final int LENGTH = LAYOUT.length();

    private CategoryBalanceRecord() {
    }

    public static CategoryBalance decode(String image, RecordSource source) {
        DecodedRecord fields = LAYOUT.decode(RecordImage.bytes(image), CodePage.FIXTURE, source);
        return new CategoryBalance(
                fields.number("TRANCAT-ACCT-ID").longValueExact(),
                fields.text("TRANCAT-TYPE-CD"),
                fields.number("TRANCAT-CD").intValueExact(),
                fields.number("TRAN-CAT-BAL"));
    }

    /** TRAN-CAT-KEY: the three key fields, 17 bytes. */
    public static String key(String image) {
        return image.substring(0, LAYOUT.field("TRANCAT-CD").end());
    }
}
