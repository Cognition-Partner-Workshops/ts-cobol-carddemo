package com.carddemo.interest.codec;

import java.math.BigDecimal;
import java.util.Optional;

import com.carddemo.interest.domain.Account;

/**
 * Layout CVACT01Y (app/cpy/CVACT01Y.cpy:4-17, RECLN 300). Data-map rows CVACT01Y/*.
 * The calculation reads ACCT-ID, ACCT-CURR-BAL and ACCT-GROUP-ID, and ACCT-ACTIVE-STATUS only under
 * inactiveAccounts=SKIP (INT-H04 option B); every other byte of the record, the 178-byte FILLER
 * included, is carried untouched (E4).
 */
public final class AccountRecord {

    public static final RecordLayout LAYOUT = RecordLayout.of("CVACT01Y")
            .unsigned("ACCT-ID", 11)
            .text("ACCT-ACTIVE-STATUS", 1)
            .signed("ACCT-CURR-BAL", 10, 2)
            .signed("ACCT-CREDIT-LIMIT", 10, 2)
            .signed("ACCT-CASH-CREDIT-LIMIT", 10, 2)
            .text("ACCT-OPEN-DATE", 10)
            .text("ACCT-EXPIRAION-DATE", 10)
            .text("ACCT-REISSUE-DATE", 10)
            .signed("ACCT-CURR-CYC-CREDIT", 10, 2)
            .signed("ACCT-CURR-CYC-DEBIT", 10, 2)
            .text("ACCT-ADDR-ZIP", 10)
            .text("ACCT-GROUP-ID", 10)
            .filler(178)
            .build();
    public static final int LENGTH = LAYOUT.length();

    private static final RecordCodec FIXTURE = new RecordCodec(LAYOUT, CodePage.fixture());
    /** INT-H04 option B: an account is active when its flag is Y (rules.yaml INT-H04, CVACT01Y.cpy:6). */
    private static final String ACTIVE = "Y";
    private static final int KEY_LENGTH = LAYOUT.field("ACCT-ID").length();

    private AccountRecord() {
    }

    public static Account from(RecordValues values) {
        return new Account(
                values.unsigned("ACCT-ID"),
                values.signed("ACCT-CURR-BAL"),
                values.text("ACCT-GROUP-ID"),
                Optional.of(ACTIVE.equals(values.text("ACCT-ACTIVE-STATUS"))));
    }

    /** Decodes a record image of the fixture code page. */
    public static Account decode(String image) {
        return decode(image, RecordOrigin.IN_MEMORY);
    }

    public static Account decode(String image, RecordOrigin origin) {
        return from(FIXTURE.decode(FixtureCodePage.bytes(image), origin));
    }

    public static String key(String image) {
        return image.substring(0, KEY_LENGTH);
    }

    /** INT-R07: rewrites the three fields of the account break and nothing else (standards S3.6). */
    public static String withInterestPosted(String image, BigDecimal newBalance) {
        return FixtureCodePage.image(withInterestPosted(FIXTURE, FixtureCodePage.bytes(image), newBalance,
                RecordOrigin.IN_MEMORY));
    }

    public static byte[] withInterestPosted(RecordCodec codec, byte[] record, BigDecimal newBalance, RecordOrigin origin) {
        byte[] updated = codec.withField(record, "ACCT-CURR-BAL", newBalance, origin);
        updated = codec.withField(updated, "ACCT-CURR-CYC-CREDIT", BigDecimal.ZERO, origin);
        return codec.withField(updated, "ACCT-CURR-CYC-DEBIT", BigDecimal.ZERO, origin);
    }
}
