package com.carddemo.interest.export;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.export.ExportSource.AccountRow;
import com.carddemo.interest.export.ExportSource.TransactionRow;

/**
 * The mapping of a table row to the record of its copybook layout: data-map rows CVTRA05Y/* and
 * CVACT01Y/*. The copybook field names of the export are here and nowhere else (standards S2.2);
 * every encoding rule is the codec's (S3.5). A FILLER is the raw bytes of its column, written
 * back byte for byte in either format (E4).
 */
final class ExportRecords {

    private final RecordCodec transactionCodec;
    private final RecordCodec accountCodec;

    ExportRecords(RecordFormat format) {
        this.transactionCodec = format.codec(TransactionRecord.LAYOUT);
        this.accountCodec = format.codec(AccountRecord.LAYOUT);
    }

    byte[] transaction(TransactionRow row, RecordOrigin origin) {
        RecordValues values = RecordValues.blank(TransactionRecord.LAYOUT)
                .with("TRAN-ID", row.id())
                .with("TRAN-TYPE-CD", row.typeCode())
                .with("TRAN-CAT-CD", row.categoryCode())
                .with("TRAN-SOURCE", row.source())
                .with("TRAN-DESC", row.description())
                .with("TRAN-AMT", row.amount())
                .with("TRAN-MERCHANT-ID", row.merchantId())
                .with("TRAN-MERCHANT-NAME", row.merchantName())
                .with("TRAN-MERCHANT-CITY", row.merchantCity())
                .with("TRAN-MERCHANT-ZIP", row.merchantZip())
                .with("TRAN-CARD-NUM", row.cardNumber())
                .with("TRAN-ORIG-TS", row.originTimestamp())
                .with("TRAN-PROC-TS", row.processedTimestamp());
        return withFiller(transactionCodec.encode(values, origin), TransactionRecord.LAYOUT, row.filler(), origin);
    }

    byte[] account(AccountRow row, RecordOrigin origin) {
        RecordValues values = RecordValues.blank(AccountRecord.LAYOUT)
                .with("ACCT-ID", row.accountId())
                .with("ACCT-ACTIVE-STATUS", row.activeStatus())
                .with("ACCT-CURR-BAL", row.currentBalance())
                .with("ACCT-CREDIT-LIMIT", row.creditLimit())
                .with("ACCT-CASH-CREDIT-LIMIT", row.cashCreditLimit())
                .with("ACCT-OPEN-DATE", row.openDate())
                .with("ACCT-EXPIRAION-DATE", row.expirationDate())
                .with("ACCT-REISSUE-DATE", row.reissueDate())
                .with("ACCT-CURR-CYC-CREDIT", row.cycleCredit())
                .with("ACCT-CURR-CYC-DEBIT", row.cycleDebit())
                .with("ACCT-ADDR-ZIP", row.zip())
                .with("ACCT-GROUP-ID", row.groupId());
        return withFiller(accountCodec.encode(values, origin), AccountRecord.LAYOUT, row.filler(), origin);
    }

    /**
     * E4: the raw bytes of the filler column at the place the layout gives the FILLER. RecordValues
     * has no way to hand a FILLER to the codec (reported in the ticket's report), so the bytes are
     * placed after the encoding, at the offset and length of the layout.
     */
    private static byte[] withFiller(byte[] record, RecordLayout layout, byte[] filler, RecordOrigin origin) {
        FieldLayout field = layout.fields().stream().filter(f -> f.kind() == Kind.FILLER).findFirst().orElseThrow();
        if (filler.length != field.length()) {
            throw new ExportRefusedException(origin + " record " + layout.copybook() + ": the FILLER is "
                    + field.length() + " bytes and the column holds " + filler.length);
        }
        System.arraycopy(filler, 0, record, field.offset(), field.length());
        return record;
    }
}
