package com.carddemo.interest.codec;

import java.math.BigDecimal;

import com.carddemo.interest.domain.Account;

/**
 * Layout CVACT01Y (app/cpy/CVACT01Y.cpy:4-17, RECLN 300). Data-map rows CVACT01Y/*.
 * The job reads ACCT-ID, ACCT-CURR-BAL and ACCT-GROUP-ID and writes three amounts; every other
 * byte of the record, the 178-byte FILLER included, is carried untouched (E4).
 */
public final class AccountRecord {

    private static final RecordLayout.Builder COPYBOOK = RecordLayout.builder("CVACT01Y");
    private static final FieldSpec ACCOUNT_ID = COPYBOOK.field("ACCT-ID", "9(11)");
    private static final FieldSpec ACTIVE_STATUS = COPYBOOK.field("ACCT-ACTIVE-STATUS", "X(01)");
    private static final FieldSpec CURRENT_BALANCE = COPYBOOK.field("ACCT-CURR-BAL", "S9(10)V99");
    private static final FieldSpec CREDIT_LIMIT = COPYBOOK.field("ACCT-CREDIT-LIMIT", "S9(10)V99");
    private static final FieldSpec CASH_CREDIT_LIMIT = COPYBOOK.field("ACCT-CASH-CREDIT-LIMIT", "S9(10)V99");
    private static final FieldSpec OPEN_DATE = COPYBOOK.field("ACCT-OPEN-DATE", "X(10)");
    private static final FieldSpec EXPIRATION_DATE = COPYBOOK.field("ACCT-EXPIRAION-DATE", "X(10)");
    private static final FieldSpec REISSUE_DATE = COPYBOOK.field("ACCT-REISSUE-DATE", "X(10)");
    private static final FieldSpec CYCLE_CREDIT = COPYBOOK.field("ACCT-CURR-CYC-CREDIT", "S9(10)V99");
    private static final FieldSpec CYCLE_DEBIT = COPYBOOK.field("ACCT-CURR-CYC-DEBIT", "S9(10)V99");
    private static final FieldSpec ZIP = COPYBOOK.field("ACCT-ADDR-ZIP", "X(10)");
    private static final FieldSpec GROUP_ID = COPYBOOK.field("ACCT-GROUP-ID", "X(10)");
    private static final FieldSpec FILLER = COPYBOOK.filler("X(178)");
    public static final RecordLayout LAYOUT = COPYBOOK.build();

    private final RecordCodec codec;

    public AccountRecord(CodePage codePage) {
        this.codec = new RecordCodec(LAYOUT, codePage);
    }

    public RecordCodec codec() {
        return codec;
    }

    /** Every field is checked at the boundary (standards S3.5), also the ones the job only carries. */
    public Account decode(byte[] record, RecordSource source) {
        RecordValues values = codec.decode(record, source);
        return new Account(
                values.unsigned(ACCOUNT_ID),
                values.signed(CURRENT_BALANCE),
                values.text(GROUP_ID));
    }

    /** INT-R07: rewrites the three fields of the account break and nothing else (standards S3.6). */
    public byte[] withInterestPosted(byte[] record, BigDecimal newBalance, RecordSource source) {
        BigDecimal zero = BigDecimal.ZERO.setScale(CYCLE_CREDIT.scale());
        byte[] updated = codec.withSigned(record, CURRENT_BALANCE, newBalance, source);
        updated = codec.withSigned(updated, CYCLE_CREDIT, zero, source);
        return codec.withSigned(updated, CYCLE_DEBIT, zero, source);
    }
}
