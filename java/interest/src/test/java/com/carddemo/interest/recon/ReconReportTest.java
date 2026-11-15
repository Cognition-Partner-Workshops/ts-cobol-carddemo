package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.jdbc.ReplicaTable;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.ExtractFormat;
import com.carddemo.interest.load.LoadFigures;

/**
 * INT-T10, the report without a database: the database side is built from the case's own files (a
 * load that kept every record) and then broken on purpose, one figure per check.
 */
class ReconReportTest {

    private static final String BASELINE = "i01-baseline";

    @TempDir
    Path temp;

    private static ReconReport.Unloaded unloaded(String caseName) {
        return ReconReporter.unloaded(Fixtures.input(caseName), ExtractFormat.FIXTURE);
    }

    private static ReconReport.Exported exported(String caseName) {
        return ReconReporter.exported(Fixtures.expected(caseName), ExtractFormat.FIXTURE);
    }

    /** What a faultless load of the case leaves in interest_run and in the tables; status as given. */
    private static DatabaseSide database(String caseName, String status) {
        ReconReport.Unloaded files = unloaded(caseName);
        Map<ReplicaTable, List<byte[]>> rows = new EnumMap<>(ReplicaTable.class);
        Map<ReplicaTable, RecordFiles> byTable = new EnumMap<>(ReplicaTable.class);
        for (ExtractFile file : ExtractFile.values()) {
            rows.put(file.table(), files.files().get(file).sources());
            byTable.put(file.table(), files.files().get(file));
        }
        LoadFigures loaded = LoadFigures.of(table -> byTable.get(table).records().size(), (table, field) -> byTable.get(table).total(field));
        ReconReport.Exported out = exported(caseName);
        boolean calculated = status.equals("CALCULATED") || status.equals("EXPORTED");
        RunRow run = new RunRow(7, status, Fixtures.PARM_DATE, loaded,
                calculated ? (long) out.transactions().records().size() : null, calculated ? out.transactions().total("TRAN-AMT") : null,
                calculated ? 1L : null, calculated ? out.accounts().total("ACCT-CURR-BAL") : null);
        if (calculated) {
            rows.put(ReplicaTable.ACCOUNT, out.accounts().sources());
        }
        return new DatabaseSide(run, rows, calculated ? (long) out.transactions().records().size() : 0L,
                calculated ? out.transactions().total("TRAN-AMT") : new BigDecimal("0.00"), null);
    }

    private static ReconReport report(String caseName, DatabaseSide database) {
        return ReconReport.of(caseName, unloaded(caseName), database, exported(caseName), CodePage.FIXTURE);
    }

    private static List<ReconLine> lines(ReconReport report, String check, Verdict verdict) {
        return report.lines().stream().filter(line -> line.check().equals(check) && line.verdict() == verdict).toList();
    }

    @ParameterizedTest
    @CsvSource({
            "i01-baseline, 2, 47.00, 3400.00, 3410.00, 10.00, 37.00",
            "i02-tie-half-cent, 2, 24.69, 2470.00, 2482.35, 12.35, 12.34",
            "i03-zero-rate-skipped, 1, 10.00, 700.00, 700.00, 0.00, 10.00",
            "i04-default-group-fallback, 1, 17.50, 1000.00, 1000.00, 0.00, 17.50",
            "i05-multi-category, 2, 17.50, 1500.00, 1500.00, 0.00, 17.50",
            "i06-negative-balance, 1, -3.00, -300.00, -300.00, 0.00, -3.00",
            "i07-nontie-rounding, 2, 29.03, 1778.10, 1793.52, 15.42, 13.61"})
    void calculatedCase_everyCheckEqual_withTheFiguresOfTheStrategyTable(String caseName, long transactions, String transactionSum,
            String before, String after, String change, String lastAccountInterest) {
        ReconReport report = report(caseName, database(caseName, "EXPORTED"));

        assertEquals(0, report.count(Verdict.DIFFERENT), report.markdown());
        assertEquals(1, report.count(Verdict.NOT_AVAILABLE), "only RC6");
        assertEquals(List.of("RC6"), report.lines().stream().filter(line -> line.verdict() == Verdict.NOT_AVAILABLE).map(ReconLine::check).toList());
        for (String check : List.of("RC1", "RC2", "RC3", "RC4", "RC5")) {
            assertFalse(lines(report, check, Verdict.EQUAL).isEmpty(), check);
        }
        String words = report.header().get(1);
        assertEquals("RC5 in words: " + transactions + " transaction(s) summing " + transactionSum + "; sum of ACCT-CURR-BAL " + before
                + " before and " + after + " after, a balance change of " + change + "; interest of the last account " + lastAccountInterest
                + ", not added to the last account (INT-Q04).", words);
        ReconLine equation = report.lines().stream().filter(line -> line.what().startsWith("sum of TRAN-AMT = change")).findFirst().orElseThrow();
        assertEquals(change + " + " + lastAccountInterest, equation.note());
        assertEquals(new BigDecimal(transactionSum), equation.left().value());
        assertEquals(new BigDecimal(transactionSum), equation.right().value());
        assertTrue(report.profile().nothingFound(), caseName);
    }

    @Test
    void everyLine_hasBothFiguresOrSaysWhyNot() {
        ReconReport report = report(BASELINE, database(BASELINE, "EXPORTED"));

        for (ReconLine line : report.lines()) {
            if (line.verdict() == Verdict.NOT_AVAILABLE) {
                assertTrue(!line.left().available() || !line.right().available());
                assertFalse((line.left().available() ? line.right() : line.left()).whyMissing().isBlank());
            } else {
                assertTrue(line.left().available() && line.right().available(), line.what());
            }
        }
        String markdown = report.markdown();
        assertTrue(markdown.contains("| RC1 | records of AWS.M2.CARDDEMO.ACCTDATA.PS.txt = rows in account | unloaded file: 2 | table: 2 | **EQUAL** |"));
        assertTrue(markdown.contains("**NOT AVAILABLE**"));
        assertTrue(markdown.contains("needs a mainframe unload after the write-back; cannot be shown off-host"));
        assertTrue(markdown.contains("## Profile of the unloaded files (TL4)"));
        for (String counted : List.of("several cards", "letter and digit type codes", "minus zero", "F-signed", "FILLER that is not blank",
                "not active", "without a rate")) {
            assertTrue(markdown.contains(counted), counted);
        }
    }

    @Test
    void loadedRunNotCalculated_rc5OnTheTablesIsNotAvailable_theEquationOfTheFilesStillShown() {
        ReconReport report = report(BASELINE, database(BASELINE, "LOADED"));

        assertEquals(0, report.count(Verdict.DIFFERENT));
        assertEquals(5, lines(report, "RC5", Verdict.NOT_AVAILABLE).size());
        assertEquals(1, lines(report, "RC5", Verdict.EQUAL).size());
        assertTrue(lines(report, "RC5", Verdict.NOT_AVAILABLE).get(0).left().whyMissing().contains("LOADED"));
    }

    // ------------------------------------------------------------ one broken figure per check

    private static DatabaseSide withRun(DatabaseSide database, UnaryOperator<RunRow> change) {
        return new DatabaseSide(change.apply(database.run()), database.tableRecords(), database.transactionRows(),
                database.transactionAmountSum(), null);
    }

    private static DatabaseSide withRows(DatabaseSide database, ReplicaTable table, UnaryOperator<List<byte[]>> change) {
        Map<ReplicaTable, List<byte[]>> rows = new EnumMap<>(database.tableRecords());
        rows.put(table, change.apply(rows.get(table)));
        return new DatabaseSide(database.run(), rows, database.transactionRows(), database.transactionAmountSum(), null);
    }

    @Test
    void rc1_brokenCount_isDifferentWithBothFigures() {
        DatabaseSide broken = withRun(database(BASELINE, "EXPORTED"), run -> new RunRow(run.runId(), run.status(), run.parmDate(),
                new LoadFigures(2, 3, 2, 15, run.loaded().acctCurrBalSum(), run.loaded().acctCreditLimitSum(),
                        run.loaded().acctCashCreditLimitSum(), run.loaded().acctCurrCycCreditSum(), run.loaded().acctCurrCycDebitSum(),
                        run.loaded().tranCatBalSum(), run.loaded().disIntRateSum(), run.loaded().acctIdSum(), run.loaded().xrefAcctIdSum(),
                        run.loaded().trancatAcctIdSum()),
                run.writtenTransactionCount(), run.writtenTranAmtSum(), run.writtenAccountCount(), run.writtenAcctCurrBalSum()));

        ReconReport report = report(BASELINE, broken);

        List<ReconLine> different = report.lines().stream().filter(line -> line.verdict() == Verdict.DIFFERENT).toList();
        assertEquals(1, different.size());
        assertEquals("RC1", different.get(0).check());
        assertEquals("2", different.get(0).left().text());
        assertEquals("3", different.get(0).right().text());
        assertTrue(report.markdown().contains("unloaded file: 2 | interest_run: 3 | **DIFFERENT**"));
    }

    @Test
    void rc2_brokenSum_isDifferent() {
        DatabaseSide broken = withRun(database(BASELINE, "EXPORTED"), run -> new RunRow(run.runId(), run.status(), run.parmDate(),
                new LoadFigures(2, 2, 2, 15, run.loaded().acctCurrBalSum(), run.loaded().acctCreditLimitSum(),
                        run.loaded().acctCashCreditLimitSum(), run.loaded().acctCurrCycCreditSum(), run.loaded().acctCurrCycDebitSum(),
                        new BigDecimal("3400.01"), run.loaded().disIntRateSum(), run.loaded().acctIdSum(), run.loaded().xrefAcctIdSum(),
                        run.loaded().trancatAcctIdSum()),
                run.writtenTransactionCount(), run.writtenTranAmtSum(), run.writtenAccountCount(), run.writtenAcctCurrBalSum()));

        ReconReport report = report(BASELINE, broken);

        List<ReconLine> different = report.lines().stream().filter(line -> line.verdict() == Verdict.DIFFERENT).toList();
        assertEquals(1, different.size());
        assertEquals("sum of TRAN-CAT-BAL", different.get(0).what());
        assertEquals("3400.00", different.get(0).left().text());
        assertEquals("3400.01", different.get(0).right().text());
    }

    @Test
    void rc3_rowThatDoesNotEncodeToItsRecord_isDifferent() {
        DatabaseSide broken = withRows(database(BASELINE, "LOADED"), ReplicaTable.CARD_XREF, rows -> {
            byte[] changed = rows.get(1).clone();
            changed[changed.length - 20] = 'X';
            return List.of(rows.get(0), changed);
        });

        ReconReport report = report(BASELINE, broken);

        List<ReconLine> different = lines(report, "RC3", Verdict.DIFFERENT);
        assertEquals(1, different.size());
        assertEquals("2", different.get(0).left().text());
        assertEquals("1", different.get(0).right().text());
        assertTrue(different.get(0).note().contains("1 record(s) with a difference no gap explains"));
        assertEquals(1, report.count(Verdict.DIFFERENT));
    }

    @Test
    void rc3_calculatedRun_theColumnsTheCalculationUpdatesAreLeftOut_anotherColumnIsNot() {
        ReconReport calculated = report(BASELINE, database(BASELINE, "CALCULATED"));
        assertEquals(4, lines(calculated, "RC3", Verdict.EQUAL).size());
        assertTrue(lines(calculated, "RC3", Verdict.EQUAL).get(0).note().contains("ACCT-CURR-BAL"));

        // The same rows under a run that is only LOADED: the changed balance is a difference.
        DatabaseSide loadedWithChangedBalances = withRows(database(BASELINE, "LOADED"), ReplicaTable.ACCOUNT,
                rows -> exported(BASELINE).accounts().sources());
        assertEquals(1, lines(report(BASELINE, loadedWithChangedBalances), "RC3", Verdict.DIFFERENT).size());
    }

    @Test
    void rc4_missingFirstRow_keyEndsAndCountsDiffer() {
        DatabaseSide broken = withRows(database(BASELINE, "LOADED"), ReplicaTable.DISCLOSURE_GROUP, rows -> rows.subList(1, rows.size()));

        ReconReport report = report(BASELINE, broken);

        List<ReconLine> different = lines(report, "RC4", Verdict.DIFFERENT);
        assertEquals(1, different.size());
        assertTrue(different.get(0).what().startsWith("lowest and highest key of disclosure_group"));
        assertEquals(1, lines(report, "RC1", Verdict.DIFFERENT).size());
        assertEquals(1, lines(report, "RC3", Verdict.DIFFERENT).size());
    }

    @Test
    void rc4_repeatedKey_distinctKeysDifferFromRows() {
        DatabaseSide broken = withRows(database(BASELINE, "LOADED"), ReplicaTable.ACCOUNT, rows -> List.of(rows.get(0), rows.get(1), rows.get(1)));

        ReconLine line = lines(report(BASELINE, broken), "RC4", Verdict.DIFFERENT).get(0);

        assertEquals("2", line.left().text());
        assertEquals("3", line.right().text());
    }

    @Test
    void rc5_brokenTransactionCountAndSum_areDifferent() {
        DatabaseSide database = database(BASELINE, "EXPORTED");
        DatabaseSide broken = new DatabaseSide(database.run(), database.tableRecords(), 3L, new BigDecimal("47.01"), null);

        List<ReconLine> different = lines(report(BASELINE, broken), "RC5", Verdict.DIFFERENT);

        assertEquals(2, different.size());
        assertEquals("3", different.get(0).left().text());
        assertEquals("2", different.get(0).right().text());
        assertEquals("47.01", different.get(1).left().text());
        assertEquals("47.00", different.get(1).right().text());
    }

    @Test
    void rc5_lastAccountUpdatedInTheExport_theEquationDoesNotHold() throws IOException {
        // An export in which the 37.00 of the last account was added to its balance (what INT-Q04 option B would write).
        Path out = temp.resolve("out");
        Files.createDirectories(out.resolve("datasets"));
        Path expected = Fixtures.expected(BASELINE).resolve("datasets");
        Files.copy(expected.resolve("AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt"), out.resolve("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt"));
        List<String> accounts = Fixtures.lines(expected.resolve("AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt"));
        String last = accounts.get(1);
        assertTrue(last.contains("0000024000{"), last);
        Files.writeString(out.resolve("datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt"),
                accounts.get(0) + "\n" + last.replaceFirst("0000024000\\{", "0000024370{") + "\n", StandardCharsets.ISO_8859_1);

        ReconReport report = ReconReport.of(BASELINE, unloaded(BASELINE), database(BASELINE, "LOADED"),
                ReconReporter.exported(out, ExtractFormat.FIXTURE), CodePage.FIXTURE);

        ReconLine equation = lines(report, "RC5", Verdict.DIFFERENT).get(0);
        assertEquals("47.00", equation.left().text());
        assertEquals("84.00", equation.right().text());
        assertEquals("47.00 + 37.00", equation.note());
        assertTrue(report.header().get(1).endsWith("the equation of RC5 does not hold."));
    }

    // ------------------------------------------------------------ figures that cannot be computed

    @Test
    void noRun_everyDatabaseFigureIsNotAvailable_neverEqual() {
        ReconReport report = report(BASELINE, new DatabaseSide(null, Map.of(), null, null, null));

        assertEquals(0, report.count(Verdict.DIFFERENT));
        assertEquals(1, report.count(Verdict.EQUAL), "only the RC5 equation, which needs the files alone");
        assertEquals("Run: no row in interest_run", report.header().get(0));
        assertTrue(report.markdown().contains("NOT AVAILABLE (no row in interest_run)"));
    }

    @Test
    void failedRun_theTablesAreNotComparedWithTheFiles() {
        DatabaseSide failed = new DatabaseSide(new RunRow(9, "FAILED", Fixtures.PARM_DATE, null, null, null, null, null), Map.of(), 0L,
                new BigDecimal("0.00"), null);

        ReconReport report = report(BASELINE, failed);

        assertEquals(0, lines(report, "RC1", Verdict.EQUAL).size());
        assertEquals(8, lines(report, "RC1", Verdict.NOT_AVAILABLE).size());
        assertTrue(report.markdown().contains("interest_run 9 is FAILED: the tables do not hold its load"));
    }

    @Test
    void databaseNotReadable_isNotAvailableWithTheReason() {
        ReconReport report = report(BASELINE, DatabaseSide.unavailable("the database cannot be read: connection refused"));

        assertEquals(0, report.count(Verdict.DIFFERENT));
        assertTrue(report.header().get(0).contains("connection refused"));
        assertEquals(10, lines(report, "RC2", Verdict.NOT_AVAILABLE).size());
    }

    @Test
    void noExportedFiles_rc5IsNotAvailable() {
        ReconReport report = ReconReport.of(BASELINE, unloaded(BASELINE), database(BASELINE, "EXPORTED"),
                ReconReporter.exported(temp.resolve("nothing-here"), ExtractFormat.FIXTURE), CodePage.FIXTURE);

        assertEquals(6, lines(report, "RC5", Verdict.NOT_AVAILABLE).size());
        assertEquals(0, lines(report, "RC5", Verdict.EQUAL).size());
        assertTrue(report.header().get(1).contains("NOT AVAILABLE (no file AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt)"));
        assertFalse(report.header().get(1).contains("not added"));
    }

    @Test
    void unreadableUnloadedFile_itsFiguresAndTheProfileAreNotAvailable() throws IOException {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        for (ExtractFile file : ExtractFile.values()) {
            Files.copy(Fixtures.input(BASELINE).resolve(file.fileName(ExtractFormat.FIXTURE)), input.resolve(file.fileName(ExtractFormat.FIXTURE)));
        }
        Path balances = input.resolve(ExtractFile.BALANCES.fileName(ExtractFormat.FIXTURE));
        List<String> lines = Fixtures.lines(balances);
        Files.writeString(balances, lines.get(0) + "\n" + lines.get(1).substring(0, 17) + "ABCDEFGHIJK" + lines.get(1).substring(28) + "\n",
                StandardCharsets.ISO_8859_1);

        ReconReport report = ReconReport.of(BASELINE, ReconReporter.unloaded(input, ExtractFormat.FIXTURE), database(BASELINE, "EXPORTED"),
                exported(BASELINE), CodePage.FIXTURE);

        assertEquals(0, report.count(Verdict.DIFFERENT));
        assertNull(report.profile());
        assertTrue(report.markdown().contains("NOT AVAILABLE (an unloaded file cannot be read)"));
        assertTrue(lines(report, "RC2", Verdict.NOT_AVAILABLE).stream().anyMatch(line -> line.what().equals("sum of TRAN-CAT-BAL")));
        assertTrue(lines(report, "RC5", Verdict.NOT_AVAILABLE).stream().anyMatch(line -> line.what().startsWith("sum of TRAN-AMT = change")));
    }

    @Test
    void figure_missingEqualsNothing_amountsCompareByValue() {
        assertFalse(Figure.missing("why").sameAs(Figure.missing("why")));
        assertFalse(Figure.of(1L).sameAs(Figure.missing("why")));
        assertTrue(Figure.of(new BigDecimal("4003")).sameAs(Figure.of(new BigDecimal("4003.00"))));
        assertFalse(Figure.of(new BigDecimal("47.00")).sameAs(Figure.of(new BigDecimal("47.01"))));
        assertEquals(Verdict.NOT_AVAILABLE, ReconLine.compare("RC1", "x", "a", Figure.missing("why"), "b", Figure.missing("why")).verdict());
        assertEquals("NOT AVAILABLE (not recorded)", Figure.of((Long) null).text());
    }

    @Test
    void profile_countsWhatTheGapsAskFor() throws IOException {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        for (ExtractFile file : ExtractFile.values()) {
            Files.copy(Fixtures.input(BASELINE).resolve(file.fileName(ExtractFormat.FIXTURE)), input.resolve(file.fileName(ExtractFormat.FIXTURE)));
        }
        // A second card for account 2001 (G08), and an inactive account (INT-H04).
        Path cards = input.resolve(ExtractFile.CARDS.fileName(ExtractFormat.FIXTURE));
        List<String> cardLines = Fixtures.lines(cards);
        Files.writeString(cards, String.join("\n", cardLines) + "\n" + "4000000000002009" + cardLines.get(0).substring(16) + "\n",
                StandardCharsets.ISO_8859_1);
        Path accounts = input.resolve(ExtractFile.ACCOUNTS.fileName(ExtractFormat.FIXTURE));
        List<String> accountLines = Fixtures.lines(accounts);
        assertEquals('Y', accountLines.get(0).charAt(11));
        Files.writeString(accounts, accountLines.get(0).substring(0, 11) + "N" + accountLines.get(0).substring(12) + "\n" + accountLines.get(1) + "\n",
                StandardCharsets.ISO_8859_1);

        ReconReport.Unloaded files = ReconReporter.unloaded(input, ExtractFormat.FIXTURE);
        TrialLoadProfile profile = TrialLoadProfile.of(files.files().get(ExtractFile.ACCOUNTS), files.files().get(ExtractFile.CARDS),
                files.files().get(ExtractFile.BALANCES), files.files().get(ExtractFile.RATES), CodePage.FIXTURE);

        assertFalse(profile.nothingFound());
        assertEquals(1, profile.accountsWithSeveralCards());
        assertEquals(1, profile.inactiveAccounts());
        assertEquals(0, profile.fSignedFields());
        assertEquals(0, profile.minusZeroFields());
    }
}
