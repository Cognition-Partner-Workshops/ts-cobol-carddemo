package com.carddemo.interest.codec;

import com.carddemo.interest.domain.InterestTransaction;

/**
 * Layout CVTRA05Y (app/cpy/CVTRA05Y.cpy:4-18, RECLN 350). Data-map rows CVTRA05Y/*.
 * The tail of TRAN-DESC and the 20-byte FILLER are never set by CBACT04C (485-489, INT-T04);
 * they are written as spaces (gap G05).
 */
public final class TransactionRecord {

    public static final RecordLayout LAYOUT = RecordLayout.of("CVTRA05Y")
            .text("TRAN-ID", 16)
            .text("TRAN-TYPE-CD", 2)
            .unsigned("TRAN-CAT-CD", 4)
            .text("TRAN-SOURCE", 10)
            .text("TRAN-DESC", 100)
            .signed("TRAN-AMT", 9, 2)
            .unsigned("TRAN-MERCHANT-ID", 9)
            .text("TRAN-MERCHANT-NAME", 50)
            .text("TRAN-MERCHANT-CITY", 50)
            .text("TRAN-MERCHANT-ZIP", 10)
            .text("TRAN-CARD-NUM", 16)
            .text("TRAN-ORIG-TS", 26)
            .text("TRAN-PROC-TS", 26)
            .filler(20)
            .build();
    public static final int LENGTH = LAYOUT.length();

    private static final RecordCodec FIXTURE = new RecordCodec(LAYOUT, CodePage.fixture());

    private TransactionRecord() {
    }

    /** The values of a new interest transaction: merchant name, city, zip and FILLER stay spaces. */
    public static RecordValues values(InterestTransaction transaction) {
        return RecordValues.blank(LAYOUT)
                .with("TRAN-ID", transaction.id())
                .with("TRAN-TYPE-CD", transaction.typeCode())
                .with("TRAN-CAT-CD", (long) transaction.categoryCode())
                .with("TRAN-SOURCE", transaction.source())
                .with("TRAN-DESC", transaction.description())
                .with("TRAN-AMT", transaction.amount())
                .with("TRAN-MERCHANT-ID", transaction.merchantId())
                .with("TRAN-CARD-NUM", transaction.cardNumber())
                .with("TRAN-ORIG-TS", transaction.originTimestamp())
                .with("TRAN-PROC-TS", transaction.processedTimestamp());
    }

    /** What an interest transaction holds of a record; the merchant texts and the FILLER are not part of it. */
    public static InterestTransaction from(RecordValues values) {
        return new InterestTransaction(
                values.text("TRAN-ID"),
                values.text("TRAN-TYPE-CD"),
                (int) values.unsigned("TRAN-CAT-CD"),
                values.text("TRAN-SOURCE"),
                values.text("TRAN-DESC"),
                values.signed("TRAN-AMT"),
                values.unsigned("TRAN-MERCHANT-ID"),
                values.text("TRAN-CARD-NUM"),
                values.text("TRAN-ORIG-TS"),
                values.text("TRAN-PROC-TS"));
    }

    /** Encodes a record image of the fixture code page. */
    public static String encode(InterestTransaction transaction) {
        return encode(transaction, RecordOrigin.IN_MEMORY);
    }

    public static String encode(InterestTransaction transaction, RecordOrigin origin) {
        return FixtureCodePage.image(FIXTURE.encode(values(transaction), origin));
    }

    /** Decodes a record image of the fixture code page. */
    public static InterestTransaction decode(String image, RecordOrigin origin) {
        return from(FIXTURE.decode(FixtureCodePage.bytes(image), origin));
    }
}
