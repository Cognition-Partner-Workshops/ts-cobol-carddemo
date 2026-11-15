package com.carddemo.interest.codec;

/** Layout CVACT03Y (app/cpy/CVACT03Y.cpy:4-8, RECLN 50). Data-map rows CVACT03Y/*. */
public final class CardXrefRecord {

    public static final RecordLayout LAYOUT = RecordLayout.of("CVACT03Y")
            .text("XREF-CARD-NUM", 16)
            .unsigned("XREF-CUST-ID", 9)
            .unsigned("XREF-ACCT-ID", 11)
            .filler(14)
            .build();
    public static final int LENGTH = LAYOUT.length();

    private static final RecordCodec FIXTURE = new RecordCodec(LAYOUT, CodePage.fixture());

    private CardXrefRecord() {
    }

    public record CardOfAccount(long accountId, String cardNumber) {
    }

    public static CardOfAccount from(RecordValues values) {
        return new CardOfAccount(values.unsigned("XREF-ACCT-ID"), values.text("XREF-CARD-NUM"));
    }

    /** Decodes a record image of the fixture code page. */
    public static CardOfAccount decode(String image) {
        return decode(image, RecordOrigin.IN_MEMORY);
    }

    public static CardOfAccount decode(String image, RecordOrigin origin) {
        return from(FIXTURE.decode(FixtureCodePage.bytes(image), origin));
    }
}
