package com.carddemo.interest.codec;

import java.math.BigDecimal;

import com.carddemo.interest.domain.InterestTransaction;

/**
 * Layout CVTRA05Y (app/cpy/CVTRA05Y.cpy:4-18, RECLN 350). Data-map rows CVTRA05Y/*.
 * The tail of TRAN-DESC and the 20-byte FILLER are never set by CBACT04C (485-489, INT-T04);
 * they are written as spaces (gap G05), as are the three merchant text fields.
 */
public final class TransactionRecord {

    public static final RecordLayout LAYOUT = CardDemoLayouts.TRANSACTION;
    public static final int LENGTH = LAYOUT.length();

    private static final RecordSource NEW_RECORD = RecordSource.built("SYSTRAN");

    private TransactionRecord() {
    }

    public static String encode(InterestTransaction transaction) {
        DecodedRecord fields = LAYOUT.blank(CodePage.FIXTURE)
                .with("TRAN-ID", transaction.id())
                .with("TRAN-TYPE-CD", transaction.typeCode())
                .with("TRAN-CAT-CD", BigDecimal.valueOf(transaction.categoryCode()))
                .with("TRAN-SOURCE", transaction.source())
                .with("TRAN-DESC", transaction.description())
                .with("TRAN-AMT", transaction.amount())
                .with("TRAN-MERCHANT-ID", BigDecimal.valueOf(transaction.merchantId()))
                .with("TRAN-CARD-NUM", transaction.cardNumber())
                .with("TRAN-ORIG-TS", transaction.originTimestamp())
                .with("TRAN-PROC-TS", transaction.processedTimestamp());
        return RecordImage.image(LAYOUT.encode(fields, CodePage.FIXTURE, NEW_RECORD));
    }
}
