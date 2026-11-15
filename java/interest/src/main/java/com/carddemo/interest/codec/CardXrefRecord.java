package com.carddemo.interest.codec;

/** Layout CVACT03Y (app/cpy/CVACT03Y.cpy:4-8, RECLN 50). Data-map rows CVACT03Y/XREF-CARD-NUM, XREF-ACCT-ID. */
public final class CardXrefRecord {

    public static final int LENGTH = 50;

    private static final Field CARD_NUMBER = new Field("XREF-CARD-NUM", 0, 16);
    private static final Field ACCOUNT_ID = new Field("XREF-ACCT-ID", 25, 11);

    private CardXrefRecord() {
    }

    public record CardOfAccount(long accountId, String cardNumber) {
    }

    public static CardOfAccount decode(String image) {
        return new CardOfAccount(ACCOUNT_ID.unsigned(image), CARD_NUMBER.text(image));
    }
}
