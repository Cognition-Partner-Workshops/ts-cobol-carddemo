package com.carddemo.interest.codec;

/** Layout CVACT03Y (app/cpy/CVACT03Y.cpy:4-8, RECLN 50). Data-map rows CVACT03Y/XREF-CARD-NUM, XREF-ACCT-ID. */
public final class CardXrefRecord {

    public static final RecordLayout LAYOUT = CardDemoLayouts.CARD_XREF;
    public static final int LENGTH = LAYOUT.length();

    private CardXrefRecord() {
    }

    public record CardOfAccount(long accountId, String cardNumber) {
    }

    public static CardOfAccount decode(String image, RecordSource source) {
        DecodedRecord fields = LAYOUT.decode(RecordImage.bytes(image), CodePage.FIXTURE, source);
        return new CardOfAccount(fields.number("XREF-ACCT-ID").longValueExact(), fields.text("XREF-CARD-NUM"));
    }
}
