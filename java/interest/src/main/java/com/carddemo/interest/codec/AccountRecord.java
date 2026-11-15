package com.carddemo.interest.codec;

import java.math.BigDecimal;

import com.carddemo.interest.domain.Account;

/**
 * Layout CVACT01Y (app/cpy/CVACT01Y.cpy:4-17, RECLN 300) mapped to the business record Account.
 * Data-map rows CVACT01Y/ACCT-ID, ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT, ACCT-GROUP-ID.
 * Images are records of the fixture format ({@link RecordImage}).
 */
public final class AccountRecord {

    public static final RecordLayout LAYOUT = CardDemoLayouts.ACCOUNT;
    public static final int LENGTH = LAYOUT.length();

    private static final String ACCOUNT_ID = "ACCT-ID";
    private static final String CURRENT_BALANCE = "ACCT-CURR-BAL";
    private static final String CYCLE_CREDIT = "ACCT-CURR-CYC-CREDIT";
    private static final String CYCLE_DEBIT = "ACCT-CURR-CYC-DEBIT";
    private static final String GROUP_ID = "ACCT-GROUP-ID";

    private AccountRecord() {
    }

    public static Account decode(String image, RecordSource source) {
        DecodedRecord fields = LAYOUT.decode(RecordImage.bytes(image), CodePage.FIXTURE, source);
        return new Account(
                fields.number(ACCOUNT_ID).longValueExact(),
                fields.number(CURRENT_BALANCE),
                fields.text(GROUP_ID));
    }

    public static String key(String image) {
        return image.substring(0, LAYOUT.field(ACCOUNT_ID).end());
    }

    /**
     * INT-R07: rewrites the three fields of the account break and no other byte (INT-T06, standards
     * S3.6): the fields the job does not set keep their bytes, an F-signed amount included (G02).
     */
    public static String withInterestPosted(String image, BigDecimal newBalance, RecordSource source) {
        byte[] record = RecordImage.bytes(image);
        record = LAYOUT.replace(record, CURRENT_BALANCE, newBalance, CodePage.FIXTURE, source);
        record = LAYOUT.replace(record, CYCLE_CREDIT, BigDecimal.ZERO, CodePage.FIXTURE, source);
        record = LAYOUT.replace(record, CYCLE_DEBIT, BigDecimal.ZERO, CodePage.FIXTURE, source);
        return RecordImage.image(record);
    }
}
