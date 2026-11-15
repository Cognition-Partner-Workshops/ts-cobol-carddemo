package com.carddemo.interest.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.domain.Account;

/**
 * INT-H04 option B needs the active flag of the account: decode passes ACCT-ACTIVE-STATUS
 * (CVACT01Y.cpy:6, offset 11, length 1; data-map row CVACT01Y/ACCT-ACTIVE-STATUS) to the domain.
 */
class AccountRecordActiveStatusTest {

    private static final String ACCOUNTS_INPUT = "AWS.M2.CARDDEMO.ACCTDATA.PS.txt";

    private static String image(String line) {
        return FixtureCodePage.image(
                RecordFormat.fixture().records(FixtureCodePage.bytes(line), AccountRecord.LAYOUT, "test").get(0));
    }

    private static String firstAccountOfI01() {
        // 00000002001 / Y / 0000010000{ ... GOLD: account 2001, active, 1000.00
        return Fixtures.lines(Fixtures.input("i01-baseline").resolve(ACCOUNTS_INPUT)).get(0);
    }

    private static String withFlag(String line, char flag) {
        return line.substring(0, 11) + flag + line.substring(12);
    }

    @Test
    void intH04_flagYIsAnActiveAccount() {
        assertEquals(new Account(2001L, new BigDecimal("1000.00"), "GOLD", Optional.of(true)),
                AccountRecord.decode(image(firstAccountOfI01())));
    }

    @ParameterizedTest
    @ValueSource(chars = {'N', ' ', 'y', 'X'})
    void intH04_anyOtherFlagIsNotActive(char flag) {
        // rules.yaml INT-H04 option B: "skip accounts whose active flag is not Y"
        assertEquals(new Account(2001L, new BigDecimal("1000.00"), "GOLD", Optional.of(false)),
                AccountRecord.decode(image(withFlag(firstAccountOfI01(), flag))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intH04_everyFixtureAccountIsActive(String caseName) {
        // rules.yaml INT-H04 fixtureNote: every fixture account has ACCT-ACTIVE-STATUS = Y
        List<String> lines = Fixtures.lines(Fixtures.input(caseName).resolve(ACCOUNTS_INPUT));

        assertEquals(lines.stream().map(line -> Optional.of(true)).toList(),
                lines.stream().map(line -> AccountRecord.decode(image(line)).active()).toList());
    }

    @Test
    void intR07_accountBreakLeavesTheFlagAsItWas() {
        // the encode side is unchanged: posting interest rewrites three fields and carries the flag byte
        String image = image(withFlag(firstAccountOfI01(), 'N'));

        String posted = AccountRecord.withInterestPosted(image, new BigDecimal("1010.00"));

        assertEquals("N", posted.substring(11, 12));
        assertEquals(Optional.of(false), AccountRecord.decode(posted).active());
    }
}
