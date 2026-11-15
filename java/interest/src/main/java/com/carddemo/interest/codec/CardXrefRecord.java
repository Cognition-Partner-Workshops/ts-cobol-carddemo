package com.carddemo.interest.codec;

/** Layout CVACT03Y (app/cpy/CVACT03Y.cpy:4-8, RECLN 50). Data-map rows CVACT03Y/*. */
public final class CardXrefRecord {

    private static final RecordLayout.Builder COPYBOOK = RecordLayout.builder("CVACT03Y");
    private static final FieldSpec CARD_NUMBER = COPYBOOK.field("XREF-CARD-NUM", "X(16)");
    private static final FieldSpec CUSTOMER_ID = COPYBOOK.field("XREF-CUST-ID", "9(09)");
    private static final FieldSpec ACCOUNT_ID = COPYBOOK.field("XREF-ACCT-ID", "9(11)");
    private static final FieldSpec FILLER = COPYBOOK.filler("X(14)");
    public static final RecordLayout LAYOUT = COPYBOOK.build();

    private final RecordCodec codec;

    public CardXrefRecord(CodePage codePage) {
        this.codec = new RecordCodec(LAYOUT, codePage);
    }

    public RecordCodec codec() {
        return codec;
    }

    public record CardOfAccount(long accountId, String cardNumber) {
    }

    public CardOfAccount decode(byte[] record, RecordSource source) {
        RecordValues values = codec.decode(record, source);
        return new CardOfAccount(values.unsigned(ACCOUNT_ID), values.text(CARD_NUMBER));
    }
}
