package com.carddemo.interest.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.carddemo.interest.FileCaseRun;
import com.carddemo.interest.Fixtures;
import com.carddemo.interest.PolicySwitches;
import com.carddemo.interest.policy.InterestPolicy;
import com.carddemo.interest.policy.InterestPolicy.InactiveAccounts;
import com.carddemo.interest.policy.InterestPolicy.Rounding;
import com.carddemo.interest.policy.InterestPolicy.ZeroInterest;

/**
 * Ticket 04: the calculation through the file adapters on each of the seven recorded cases. The
 * expected side is always the recorded legacy output under fixtures/interest/&lt;case&gt;/expected,
 * compared byte for byte (standards S7.4).
 */
class SevenCasesFileParityTest {

    static final String TRANSACTIONS = FileRunWorkspace.TRANSACTIONS_DATASET;
    static final String ACCOUNTS = FileRunWorkspace.ACCOUNTS_DATASET;
    static final String RUN_LOG = FileRunWorkspace.RUN_LOG_FILE;
    static final String RETURN_CODES = FileRunWorkspace.RETURN_CODES_FILE;

    @TempDir
    Path temp;

    private Path run(String caseName, InterestPolicy policy) {
        return FileCaseRun.run(Fixtures.input(caseName), temp.resolve(caseName).resolve("actual"), policy);
    }

    private static void assertSameAsRecorded(String caseName, Path output, String file) {
        assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(output.resolve(file)),
                caseName + " " + file);
    }

    private static void assertAllSameAsRecorded(String caseName, Path output) {
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG, RETURN_CODES)) {
            assertSameAsRecorded(caseName, output, file);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void datasets_matchRecorded(String caseName) {
        Path output = run(caseName, InterestPolicy.legacy());

        assertSameAsRecorded(caseName, output, TRANSACTIONS);
        assertSameAsRecorded(caseName, output, ACCOUNTS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void sysout_matchesRecorded(String caseName) {
        assertSameAsRecorded(caseName, run(caseName, InterestPolicy.legacy()), RUN_LOG);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void rcJson_matchesRecorded(String caseName) {
        assertSameAsRecorded(caseName, run(caseName, InterestPolicy.legacy()), RETURN_CODES);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ04_lastAccountNotUpdated(String caseName) {
        // the account with the highest id on the balance file keeps its input record, byte for byte
        List<String> balances = Fixtures.lines(Fixtures.input(caseName).resolve(FileRunWorkspace.BALANCES_FILE));
        String lastAccountId = balances.stream().map(line -> line.substring(0, 11)).max(String::compareTo).orElseThrow();
        String inputRecord = accountRecord(Fixtures.input(caseName).resolve(FileRunWorkspace.ACCOUNTS_FILE), lastAccountId);

        Path output = run(caseName, InterestPolicy.legacy());

        assertEquals(inputRecord, accountRecord(output.resolve(ACCOUNTS), lastAccountId));
        // ... while it did get its interest transaction
        assertTrue(Fixtures.text(output.resolve(TRANSACTIONS)).contains("Int. for a/c " + lastAccountId));
    }

    private static String accountRecord(Path file, String accountId) {
        return Fixtures.lines(file).stream().filter(line -> line.startsWith(accountId)).findFirst().orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i07-nontie-rounding"})
    void intR07_updatesAccountAtBreak(String caseName) {
        // the first account of these cases is not the last: its recorded record differs from its input record
        // in ACCT-CURR-BAL (12-24), ACCT-CURR-CYC-CREDIT (78-90) and ACCT-CURR-CYC-DEBIT (90-102) only
        String input = Fixtures.lines(Fixtures.input(caseName).resolve(FileRunWorkspace.ACCOUNTS_FILE)).get(0);
        String recorded = Fixtures.lines(Fixtures.expected(caseName).resolve(ACCOUNTS)).get(0);

        String actual = Fixtures.lines(run(caseName, InterestPolicy.legacy()).resolve(ACCOUNTS)).get(0);

        assertEquals(recorded, actual);
        assertNotEquals(input.substring(12, 24), actual.substring(12, 24));
        assertEquals("00000000000{00000000000{", actual.substring(78, 102));
        assertEquals(input.substring(0, 12) + input.substring(24, 78) + input.substring(102),
                actual.substring(0, 12) + actual.substring(24, 78) + actual.substring(102));
    }

    @Test
    void intQ01_truncatesHalfCent() {
        // ticket 04: i02-tie-half-cent writes TRAN-AMT 12.35 for balance 00000002003/01/0001
        // (1235.50 * 12.00 / 1200 = 12.355), zoned as 0000000123E
        String transaction = Fixtures.lines(run("i02-tie-half-cent", InterestPolicy.legacy()).resolve(TRANSACTIONS)).get(0);

        assertEquals("Int. for a/c 00000002003", transaction.substring(32, 56));
        assertEquals("0000000123E", transaction.substring(132, 143));
    }

    @Test
    void intQ01_halfEvenFailsTheTieCaseOnTranAmtOnly() {
        // ticket 04 switch test: with HALF_EVEN i02-tie-half-cent differs on TRAN-AMT (12.355 -> 12.36 = 0000000123F)
        // and on nothing else of the transaction records
        String caseName = "i02-tie-half-cent";
        List<String> recorded = Fixtures.lines(Fixtures.expected(caseName).resolve(TRANSACTIONS));

        List<String> actual = Fixtures.lines(run(caseName, PolicySwitches.rounding(Rounding.HALF_EVEN)).resolve(TRANSACTIONS));

        assertEquals("0000000123E", recorded.get(0).substring(132, 143));
        assertEquals("0000000123F", actual.get(0).substring(132, 143));
        for (int i = 0; i < recorded.size(); i++) {
            assertEquals(recorded.get(i).substring(0, 132) + recorded.get(i).substring(143),
                    actual.get(i).substring(0, 132) + actual.get(i).substring(143));
        }
        // 12.345 is a tie too, and half-even keeps it at 12.34 like truncation
        assertEquals(recorded.get(1), actual.get(1));
    }

    @Test
    void intQ01_halfEvenStillPassesTheNonTieCase() {
        // ticket 04 switch test: i07-nontie-rounding has no half cent, so every output file equals the recorded one
        assertAllSameAsRecorded("i07-nontie-rounding", run("i07-nontie-rounding", PolicySwitches.rounding(Rounding.HALF_EVEN)));
    }

    @Test
    void intR04_zeroRateBalanceWritesNoTransaction() {
        // ticket 04: i03-zero-rate-skipped has two balances and writes one transaction, for 00000002005/01/0001
        List<String> transactions = Fixtures.lines(run("i03-zero-rate-skipped", InterestPolicy.legacy()).resolve(TRANSACTIONS));

        assertEquals(2, Fixtures.lines(Fixtures.input("i03-zero-rate-skipped").resolve(FileRunWorkspace.BALANCES_FILE)).size());
        assertEquals(1, transactions.size());
        assertEquals("0000000100{", transactions.get(0).substring(132, 143));
    }

    @Test
    void intR03_fallbackLinesInTheRunLog() {
        // ticket 04: i04 logs the two fallback lines after the record echo
        List<String> runLog = Fixtures.lines(run("i04-default-group-fallback", InterestPolicy.legacy()).resolve(RUN_LOG));

        assertEquals(List.of("DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE"), runLog.subList(2, 4));
    }

    @Test
    void intR03_defaultGroupKeyPadded() {
        // behaviour g: 'DEFAULT' is padded to the 10 bytes of DIS-ACCT-GROUP-ID on the file ("DEFAULT   ");
        // the lookup by the constant DEFAULT finds it: 21.00 % for 01/0001 in i04
        FileRunWorkspace workspace = FileRunWorkspace.open(
                Fixtures.input("i04-default-group-fallback"), temp.resolve("actual"), "t");

        assertTrue(Fixtures.lines(Fixtures.input("i04-default-group-fallback").resolve(FileRunWorkspace.RATES_FILE))
                .get(0).startsWith("DEFAULT   01"));
        assertEquals("21.00", workspace.rateLookup().findRate("DEFAULT", "01", 1).orElseThrow().toPlainString());
        assertTrue(workspace.rateLookup().findRate("PLATINUM", "01", 1).isEmpty());
    }

    @Test
    void intQ05_negativeInterestIsDSigned() {
        // ticket 04: i06 writes -3.00 (-300.00 * 12.00 / 1200); zoned with the sign in the last byte: 0000000030}
        String transaction = Fixtures.lines(run("i06-negative-balance", InterestPolicy.legacy()).resolve(TRANSACTIONS)).get(0);

        assertEquals("0000000030}", transaction.substring(132, 143));
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intQ06_skipChangesNothingOnTheRecordedCases(String caseName) {
        // no recorded case has an interest of 0.00 at a non-zero rate, and modernization/rules/interest/expected
        // lists no value for INT-Q06: the switch changes nothing
        assertAllSameAsRecorded(caseName, run(caseName, PolicySwitches.zeroInterest(ZeroInterest.SKIP)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intH04_skipChangesNothingOnTheRecordedCases(String caseName) {
        // every recorded account is active, and the expected files list no value for INT-H04: the switch changes nothing
        assertAllSameAsRecorded(caseName, run(caseName, PolicySwitches.inactiveAccounts(InactiveAccounts.SKIP)));
    }

    @Test
    void intT09_emptyInputWritesNothing() throws IOException {
        // no fixture: an empty balance file. No transaction, every account record as on input, start and end line only
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        for (String name : List.of(FileRunWorkspace.ACCOUNTS_FILE, FileRunWorkspace.CARDS_FILE, FileRunWorkspace.RATES_FILE)) {
            Files.copy(Fixtures.input("i01-baseline").resolve(name), input.resolve(name));
        }
        Files.write(input.resolve(FileRunWorkspace.BALANCES_FILE), List.<String>of(), StandardCharsets.ISO_8859_1);

        Path output = FileCaseRun.run(input, temp.resolve("actual"), InterestPolicy.legacy());

        assertEquals("", Fixtures.text(output.resolve(TRANSACTIONS)));
        assertEquals(Fixtures.lines(input.resolve(FileRunWorkspace.ACCOUNTS_FILE)).stream().map(String::stripTrailing).toList(),
                Fixtures.lines(output.resolve(ACCOUNTS)).stream().map(String::stripTrailing).toList());
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "END OF EXECUTION OF PROGRAM CBACT04C"),
                Fixtures.lines(output.resolve(RUN_LOG)));
        assertSameAsRecorded("i01-baseline", output, RETURN_CODES);
    }
}
