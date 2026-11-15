package com.carddemo.interest.codec;

import java.math.BigDecimal;

import com.carddemo.interest.domain.Account;

/**
 * Layout CVACT01Y (app/cpy/CVACT01Y.cpy:4-17, RECLN 300). Data-map rows CVACT01Y/ACCT-ID,
 * ACCT-ACTIVE-STATUS (read for INT-H04 only), ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT, ACCT-GROUP-ID.
 * Every other byte of the record, the 178-byte FILLER included, is carried in the image untouched (E4).
 */
public final class AccountRecord {

    public static final int LENGTH = 300;

    private static final Field ACCOUNT_ID = new Field("ACCT-ID", 0, 11);
    private static final Field ACTIVE_STATUS = new Field("ACCT-ACTIVE-STATUS", 11, 1);
    private static final Field CURRENT_BALANCE = new Field("ACCT-CURR-BAL", 12, 12);
    private static final Field CYCLE_CREDIT = new Field("ACCT-CURR-CYC-CREDIT", 78, 12);
    private static final Field CYCLE_DEBIT = new Field("ACCT-CURR-CYC-DEBIT", 90, 12);
    private static final Field GROUP_ID = new Field("ACCT-GROUP-ID", 112, 10);

    private AccountRecord() {
    }

    public static Account decode(String image) {
        return new Account(
                ACCOUNT_ID.unsigned(image),
                ACTIVE_STATUS.text(image),
                CURRENT_BALANCE.signed(image, 2),
                GROUP_ID.text(image));
    }

    public static String key(String image) {
        return image.substring(ACCOUNT_ID.offset(), ACCOUNT_ID.end());
    }

    /** INT-R07: rewrites the three fields of the account break and nothing else (standards S3.6). */
    public static String withInterestPosted(String image, BigDecimal newBalance) {
        StringBuilder updated = new StringBuilder(image);
        replace(updated, CURRENT_BALANCE, CURRENT_BALANCE.encodeSigned(newBalance, 2));
        replace(updated, CYCLE_CREDIT, CYCLE_CREDIT.encodeSigned(BigDecimal.ZERO, 2));
        replace(updated, CYCLE_DEBIT, CYCLE_DEBIT.encodeSigned(BigDecimal.ZERO, 2));
        return updated.toString();
    }

    private static void replace(StringBuilder image, Field field, String encoded) {
        image.replace(field.offset(), field.end(), encoded);
    }
}
