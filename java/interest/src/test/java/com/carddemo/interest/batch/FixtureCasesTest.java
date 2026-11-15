package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.ExitStatus;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.Rounding;

/**
 * The calculation with the legacy policy on the seven recorded cases, through the file adapters.
 * Every expected value is the recorded legacy output under fixtures/interest/<case>/expected
 * (standards S7.4) or is worked by hand in the comment.
 */
class FixtureCasesTest {

    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final String RETURN_CODES = "rc.json";

    @TempDir
    Path temp;

    private Path run(String caseName, InterestPolicy policy) {
        Path output = temp.resolve(caseName);
        assertEquals(ExitStatus.COMPLETED, FixtureRun.run(Fixtures.input(caseName), output, policy));
        return output;
    }

    private Path run(String caseName) {
        return run(caseName, InterestPolicy.legacy());
    }

    private static List<String> recorded(String caseName, String file) {
        return Fixtures.lines(Fixtures.expected(caseName).resolve(file));
    }

    /** TRAN-AMT, S9(09)V99 at offset 132 of CVTRA05Y (app/cpy/CVTRA05Y.cpy:10), as written. */
    private static String amountImage(String transaction) {
        return transaction.substring(132, 143);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped",
            "i04-default-group-fallback", "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void allSevenCases_outputEqualsTheRecordedLegacyRun(String caseName) {
        Path output = run(caseName);

        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG, RETURN_CODES)) {
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(output.resolve(file)),
                    caseName + " " + file);
        }
    }

    @Test
    void intQ01_i02WritesTheTieAs1235() {
        // 1235.50 * 12.00 / 1200 = 12.355, cut to 12.35 = 0000000123E for balance 00000002003/01/0001
        String first = Fixtures.lines(run("i02-tie-half-cent").resolve(TRANSACTIONS)).get(0);

        assertEquals("Int. for a/c 00000002003", first.substring(32, 56));
        assertEquals("0000000123E", amountImage(first));
    }

    @Test
    void intQ01_halfEvenFailsI02OnTranAmtAndLeavesI07AsRecorded() {
        InterestPolicy halfEven = new InterestPolicy(Rounding.HALF_EVEN, null, null, false, null, null, null);

        List<String> i02 = Fixtures.lines(run("i02-tie-half-cent", halfEven).resolve(TRANSACTIONS));
        List<String> i02Recorded = recorded("i02-tie-half-cent", TRANSACTIONS);
        // 12.355 half-even is 12.36 = 0000000123F against the recorded 12.35; nothing else of the record differs
        assertEquals("0000000123F", amountImage(i02.get(0)));
        assertNotEquals(amountImage(i02Recorded.get(0)), amountImage(i02.get(0)));
        assertEquals(i02Recorded.get(0).substring(0, 132), i02.get(0).substring(0, 132));
        assertEquals(i02Recorded.get(0).substring(143), i02.get(0).substring(143));
        // 1234.50 * 12.00 / 1200 = 12.345 half-even is 12.34, as recorded
        assertEquals(i02Recorded.get(1), i02.get(1));

        // i07 has no tie: every file is as recorded under HALF_EVEN too
        Path i07 = run("i07-nontie-rounding", halfEven);
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG, RETURN_CODES)) {
            assertEquals(Fixtures.text(Fixtures.expected("i07-nontie-rounding").resolve(file)),
                    Fixtures.text(i07.resolve(file)), file);
        }
    }

    @Test
    void intR04_i03WritesNoTransactionForTheZeroRateBalance() {
        Path output = run("i03-zero-rate-skipped");

        // two balances are read and displayed (01/0001 at 12.00 %, 02/0001 at 0.00 %); one transaction is written
        assertEquals(4, Fixtures.lines(output.resolve(RUN_LOG)).size());
        List<String> transactions = Fixtures.lines(output.resolve(TRANSACTIONS));
        assertEquals(1, transactions.size());
        // 1000.00 * 12.00 / 1200 = 10.00
        assertEquals("0000000100{", amountImage(transactions.get(0)));
    }

    @Test
    void intR03_i04LogsTheFallbackAndUsesTheDefaultRate() {
        Path output = run("i04-default-group-fallback");

        assertEquals(List.of("DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE"),
                Fixtures.lines(output.resolve(RUN_LOG)).subList(2, 4));
        // 1000.00 * 21.00 (DEFAULT 01/0001) / 1200 = 17.50
        assertEquals("0000000175{", amountImage(Fixtures.lines(output.resolve(TRANSACTIONS)).get(0)));
    }

    @Test
    void intR03_defaultGroupKeyPadded() {
        // the rate file holds the group as CHAR(10) "DEFAULT   "; the domain asks for DEFAULT and finds it
        assertEquals("DEFAULT   01000100210{", Fixtures.lines(Fixtures.input("i04-default-group-fallback")
                .resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt")).get(0).substring(0, 22));
        assertEquals(1, Fixtures.lines(run("i04-default-group-fallback").resolve(TRANSACTIONS)).size());
    }

    @Test
    void intQ05_negativeInterestIsDSigned() {
        // i06: -300.00 * 12.00 / 1200 = -3.00, written with the minus overpunch: 0000000030}
        assertEquals("0000000030}", amountImage(Fixtures.lines(run("i06-negative-balance").resolve(TRANSACTIONS)).get(0)));
    }

    @Test
    void intR01_i05EveryCategoryOfTheAccountIsCharged() {
        // i05: 1000.00 at 12.00 % = 10.00 and 500.00 at 18.00 % = 7.50, in key order
        List<String> transactions = Fixtures.lines(run("i05-multi-category").resolve(TRANSACTIONS));

        assertEquals(List.of("0000000100{", "0000000075{"), transactions.stream().map(FixtureCasesTest::amountImage).toList());
    }

    @Test
    void intT02_runLogIsStartRecordsEnd() {
        List<String> runLog = Fixtures.lines(run("i01-baseline").resolve(RUN_LOG));

        assertEquals("START OF EXECUTION OF PROGRAM CBACT04C", runLog.get(0));
        // CBACT04C:193 displays the 50-byte record: 28 bytes of key and balance, 22 of FILLER (spaces)
        assertEquals(List.of("000000020010100010000010000{" + " ".repeat(22), "000000020020100010000024000{" + " ".repeat(22)),
                runLog.subList(1, 3));
        assertEquals("END OF EXECUTION OF PROGRAM CBACT04C", runLog.get(3));
    }

    @Test
    void intT09_emptyInputWritesNothing() throws IOException {
        Path input = temp.resolve("empty-input");
        Files.createDirectories(input);
        try (var files = Files.list(Fixtures.input("i01-baseline"))) {
            for (Path file : files.toList()) {
                Files.copy(file, input.resolve(file.getFileName()));
            }
        }
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt"), "");
        Path output = temp.resolve("empty-actual");

        assertEquals(ExitStatus.COMPLETED, FixtureRun.run(input, output, InterestPolicy.legacy()));

        // no transaction, no account touched, start and end line only, return code as on a normal run
        assertEquals("", Fixtures.text(output.resolve(TRANSACTIONS)));
        assertEquals(Fixtures.lines(input.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")), Fixtures.lines(output.resolve(ACCOUNTS)));
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "END OF EXECUTION OF PROGRAM CBACT04C"),
                Fixtures.lines(output.resolve(RUN_LOG)));
        assertEquals(Fixtures.text(Fixtures.expected("i01-baseline").resolve(RETURN_CODES)),
                Fixtures.text(output.resolve(RETURN_CODES)));
    }
}
