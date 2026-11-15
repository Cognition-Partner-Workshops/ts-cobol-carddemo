package com.carddemo.interest.codec;

import com.carddemo.interest.domain.InterestTransaction;

/**
 * Layout CVTRA05Y (app/cpy/CVTRA05Y.cpy:4-18, RECLN 350). Data-map rows CVTRA05Y/*.
 * The tail of TRAN-DESC and the 20-byte FILLER are never set by CBACT04C (485-489, INT-T04);
 * they are written as spaces (gap G05).
 */
public final class TransactionRecord {

    public static final int LENGTH = 350;

    private static final Field ID = new Field("TRAN-ID", 0, 16);
    private static final Field TYPE_CODE = new Field("TRAN-TYPE-CD", 16, 2);
    private static final Field CATEGORY_CODE = new Field("TRAN-CAT-CD", 18, 4);
    private static final Field SOURCE = new Field("TRAN-SOURCE", 22, 10);
    private static final Field DESCRIPTION = new Field("TRAN-DESC", 32, 100);
    private static final Field AMOUNT = new Field("TRAN-AMT", 132, 11);
    private static final Field MERCHANT_ID = new Field("TRAN-MERCHANT-ID", 143, 9);
    private static final Field MERCHANT_NAME = new Field("TRAN-MERCHANT-NAME", 152, 50);
    private static final Field MERCHANT_CITY = new Field("TRAN-MERCHANT-CITY", 202, 50);
    private static final Field MERCHANT_ZIP = new Field("TRAN-MERCHANT-ZIP", 252, 10);
    private static final Field CARD_NUMBER = new Field("TRAN-CARD-NUM", 262, 16);
    private static final Field ORIGIN_TIMESTAMP = new Field("TRAN-ORIG-TS", 278, 26);
    private static final Field PROCESSED_TIMESTAMP = new Field("TRAN-PROC-TS", 304, 26);
    private static final int FILLER_LENGTH = 20;

    private TransactionRecord() {
    }

    public static String encode(InterestTransaction transaction) {
        String image = ID.encodeText(transaction.id())
                + TYPE_CODE.encodeText(transaction.typeCode())
                + CATEGORY_CODE.encodeUnsigned(transaction.categoryCode())
                + SOURCE.encodeText(transaction.source())
                + DESCRIPTION.encodeText(transaction.description())
                + AMOUNT.encodeSigned(transaction.amount(), 2)
                + MERCHANT_ID.encodeUnsigned(transaction.merchantId())
                + MERCHANT_NAME.encodeText("")
                + MERCHANT_CITY.encodeText("")
                + MERCHANT_ZIP.encodeText("")
                + CARD_NUMBER.encodeText(transaction.cardNumber())
                + ORIGIN_TIMESTAMP.encodeText(transaction.originTimestamp())
                + PROCESSED_TIMESTAMP.encodeText(transaction.processedTimestamp())
                + " ".repeat(FILLER_LENGTH);
        if (image.length() != LENGTH) {
            throw new IllegalStateException("CVTRA05Y record is " + image.length() + " bytes, not " + LENGTH);
        }
        return image;
    }
}
