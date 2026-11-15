package com.carddemo.interest.codec;

import com.carddemo.interest.domain.InterestTransaction;

/**
 * Layout CVTRA05Y (app/cpy/CVTRA05Y.cpy:4-18, RECLN 350). Data-map rows CVTRA05Y/*.
 * The tail of TRAN-DESC and the 20-byte FILLER are never set by CBACT04C (485-489, INT-T04);
 * they are written as spaces (gap G05).
 */
public final class TransactionRecord {

    private static final RecordLayout.Builder COPYBOOK = RecordLayout.builder("CVTRA05Y");
    private static final FieldSpec ID = COPYBOOK.field("TRAN-ID", "X(16)");
    private static final FieldSpec TYPE_CODE = COPYBOOK.field("TRAN-TYPE-CD", "X(02)");
    private static final FieldSpec CATEGORY_CODE = COPYBOOK.field("TRAN-CAT-CD", "9(04)");
    private static final FieldSpec SOURCE = COPYBOOK.field("TRAN-SOURCE", "X(10)");
    private static final FieldSpec DESCRIPTION = COPYBOOK.field("TRAN-DESC", "X(100)");
    private static final FieldSpec AMOUNT = COPYBOOK.field("TRAN-AMT", "S9(09)V99");
    private static final FieldSpec MERCHANT_ID = COPYBOOK.field("TRAN-MERCHANT-ID", "9(09)");
    private static final FieldSpec MERCHANT_NAME = COPYBOOK.field("TRAN-MERCHANT-NAME", "X(50)");
    private static final FieldSpec MERCHANT_CITY = COPYBOOK.field("TRAN-MERCHANT-CITY", "X(50)");
    private static final FieldSpec MERCHANT_ZIP = COPYBOOK.field("TRAN-MERCHANT-ZIP", "X(10)");
    private static final FieldSpec CARD_NUMBER = COPYBOOK.field("TRAN-CARD-NUM", "X(16)");
    private static final FieldSpec ORIGIN_TIMESTAMP = COPYBOOK.field("TRAN-ORIG-TS", "X(26)");
    private static final FieldSpec PROCESSED_TIMESTAMP = COPYBOOK.field("TRAN-PROC-TS", "X(26)");
    private static final FieldSpec FILLER = COPYBOOK.filler("X(20)");
    public static final RecordLayout LAYOUT = COPYBOOK.build();

    private final RecordCodec codec;

    public TransactionRecord(CodePage codePage) {
        this.codec = new RecordCodec(LAYOUT, codePage);
    }

    public RecordCodec codec() {
        return codec;
    }

    /** The merchant texts and the FILLER are not set: they are written as spaces of the output code page (G05). */
    public byte[] encode(InterestTransaction transaction, RecordSource source) {
        RecordValues values = RecordValues.of(LAYOUT)
                .withText(ID, transaction.id())
                .withText(TYPE_CODE, transaction.typeCode())
                .withUnsigned(CATEGORY_CODE, transaction.categoryCode())
                .withText(SOURCE, transaction.source())
                .withText(DESCRIPTION, transaction.description())
                .withSigned(AMOUNT, transaction.amount())
                .withUnsigned(MERCHANT_ID, transaction.merchantId())
                .withText(CARD_NUMBER, transaction.cardNumber())
                .withText(ORIGIN_TIMESTAMP, transaction.originTimestamp())
                .withText(PROCESSED_TIMESTAMP, transaction.processedTimestamp());
        return codec.encode(values, source);
    }
}
