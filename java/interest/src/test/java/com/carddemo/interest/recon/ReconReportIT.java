package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.batch.InterestJobParameters;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.jdbc.calc.ReplicaTables;
import com.carddemo.interest.load.ExtractFile;

/**
 * The reconciliation report on PostgreSQL 15 with the schema of FI-4 (standards S7.3). Each test
 * loads and calculates a case with the job as {@code make parity-java} runs it (profile tables),
 * then reads the report. Expected figures are the reconciliation table of
 * modernization/design/strategy-interest.md, typed here by hand (standards S7.4). The "broken"
 * tests change one figure in the database after the run and expect exactly that check DIFFERENT,
 * with both figures on the line.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@ActiveProfiles("tables")
@Testcontainers
class ReconReportIT {

    private static final String BASELINE = "i01-baseline";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = ReplicaTables.postgres();

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    /** Loads, calculates and exports a case; the exported files are in the returned directory. */
    private Path loadedAndCalculated(String caseName) throws Exception {
        Path outputDir = temp.resolve(caseName + "-" + UUID.randomUUID()).resolve("actual");
        JobExecution execution = jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, Fixtures.input(caseName).toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        return outputDir;
    }

    private static ReconReport report(String caseName, Path inputDir, Path outputDir, OptionalLong runId) throws SQLException {
        RecordFormat format = RecordFormat.fixture();
        try (JdbcReconTables tables = JdbcReconTables.open(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            return Reconciliation.report(caseName, UnloadedFiles.read(inputDir, format), ExportedFiles.read(outputDir, format),
                    tables, runId);
        }
    }

    private static ReconReport report(String caseName, Path outputDir) throws SQLException {
        return report(caseName, Fixtures.input(caseName), outputDir, OptionalLong.empty());
    }

    private static CheckLine line(ReconReport report, String check, String what) {
        return report.lines(check).stream().filter(line -> line.what().equals(what)).findFirst().orElseThrow();
    }

    private static void assertOnly(ReconReport report, String brokenCheck) {
        for (String check : ReconReport.CHECKS) {
            assertEquals(check.equals(brokenCheck) ? Verdict.DIFFERENT : Verdict.EQUAL, report.verdict(check), check);
        }
        assertEquals(Verdict.DIFFERENT, report.verdict());
    }

    // The rows of the reconciliation table of strategy-interest.md, section "Reconciliation counts and sums".
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "i01-baseline,               2, 2, 2, 15, 3400.00, 3400.00, 120.50, 2, 47.00, 3410.00, 10.00, 37.00",
            "i02-tie-half-cent,          2, 2, 2, 15, 2470.00, 2470.00, 120.50, 2, 24.69, 2482.35, 12.35, 12.34",
            "i03-zero-rate-skipped,      1, 1, 2, 15, 700.00,  700.00,  120.50, 1, 10.00, 700.00,  0.00,  10.00",
            "i04-default-group-fallback, 1, 1, 1, 15, 1000.00, 1000.00, 120.50, 1, 17.50, 1000.00, 0.00,  17.50",
            "i05-multi-category,         1, 1, 2, 15, 1500.00, 1500.00, 120.50, 2, 17.50, 1500.00, 0.00,  17.50",
            "i06-negative-balance,       1, 1, 1, 15, -300.00, -300.00, 120.50, 1, -3.00, -300.00, 0.00,  -3.00",
            "i07-nontie-rounding,        2, 2, 2, 15, 1778.10, 1778.10, 120.50, 2, 29.03, 1793.52, 15.42, 13.61"})
    void intQ04_everyCase_isEqualWithTheFiguresOfTheReconciliationTable(String caseName, String accounts, String cards,
            String balances, String rates, String balanceBefore, String categoryBalances, String rateSum, String transactions,
            String amountSum, String balanceAfter, String balanceChange, String lastInterest) throws Exception {
        ReconReport report = report(caseName, loadedAndCalculated(caseName));

        assertEquals(Verdict.EQUAL, report.verdict(), report.markdown());
        for (String check : ReconReport.CHECKS) {
            assertEquals(Verdict.EQUAL, report.verdict(check), check);
            assertFalse(report.lines(check).isEmpty(), check);
        }
        Map<String, String> expected = Map.ofEntries(
                Map.entry("Accounts", accounts), Map.entry("Cards", cards), Map.entry("Balances", balances),
                Map.entry("Rates", rates), Map.entry("Sum ACCT-CURR-BAL before", balanceBefore),
                Map.entry("Sum TRAN-CAT-BAL", categoryBalances), Map.entry("Sum DIS-INT-RATE", rateSum),
                Map.entry("Transactions written", transactions), Map.entry("Sum TRAN-AMT", amountSum),
                Map.entry("Sum ACCT-CURR-BAL after", balanceAfter), Map.entry("Balance change", balanceChange),
                Map.entry("Interest of last account (not added)", lastInterest), Map.entry("RC5", "holds"));
        assertEquals(expected.keySet(), report.figures().keySet());
        expected.forEach((column, figure) -> assertEquals(figure, report.figure(column).text(), column));
        // RC6 needs a mainframe unload: listed, NOT AVAILABLE, never part of the result
        assertEquals(Verdict.NOT_AVAILABLE, report.verdict("RC6"));
        assertTrue(report.profile().orElseThrow().isClean());
        assertTrue(report.rejects().isEmpty());
    }

    @Test
    void intQ04_baseline_showsTwoTransactionsOf47_aChangeOf10_and37NotAddedToTheLastAccount() throws Exception {
        ReconReport report = report(BASELINE, loadedAndCalculated(BASELINE));

        CheckLine identity = line(report, "RC5", "sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04)");
        assertEquals(Verdict.EQUAL, identity.verdict());
        assertEquals("47.00", identity.first().text());
        assertEquals("47.00", identity.second().text());
        assertEquals("balance change 10.00 (3400.00 before, 3410.00 after); interest of the last account, not added: 37.00"
                + " (last account 00000002002)", identity.note());
        assertEquals("2", line(report, "RC5", "transactions written").first().text());
        assertEquals("2", line(report, "RC5", "transactions written").second().text());
        String text = report.markdown();
        assertTrue(text.contains("## Reconciliation report - i01-baseline: **EQUAL**"), text);
        assertTrue(text.contains("| 2 | 2 | 2 | 15 | 3400.00 | 3400.00 | 120.50 | 2 | 47.00 | 3410.00 | 10.00 | 37.00 | holds |"), text);
        assertTrue(text.contains("| RC6 | NOT AVAILABLE - needs ACCTDATA unloaded again on the mainframe"), text);
        // TL4 is part of the report: the eight lines of the profile
        for (String profileLine : List.of("| Accounts with several cards | G08, INT-H03 | 0 |", "| Type codes of the balances | G06 | 01 |",
                "| Accounts with mixed type codes (digits and letters) | G06 | 0 |", "| Minus zero amounts | G03 | 0 |",
                "| F-signed amounts | G02 | 0 |", "| Fillers that are not blank | E4 | 0 |",
                "| Accounts that are not active | INT-H04 | 0 |", "| Balances without a rate and without a DEFAULT rate | INT-R08 | 0 |")) {
            assertTrue(text.contains(profileLine), profileLine);
        }
    }

    @Test
    void rc1_brokenRowCount_isDifferentWithBothFigures() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        // 15 rates were loaded; one row is removed. Its key is neither the lowest nor the highest (RC4 stays EQUAL).
        assertEquals(1, jdbc.update("DELETE FROM carddemo.disclosure_group WHERE dis_acct_group_id = 'DEFAULT' AND dis_tran_type_cd = '02'"
                + " AND dis_int_rate = 0"));

        ReconReport report = report(BASELINE, outputDir);

        CheckLine rows = line(report, "RC1", "records and rows of disclosure_group");
        assertEquals(Verdict.DIFFERENT, rows.verdict());
        assertEquals("15", rows.first().text());
        assertEquals("14", rows.second().text());
        assertEquals(Verdict.DIFFERENT, line(report, "RC1", "count recorded at the load of disclosure_group").verdict());
        assertEquals(Verdict.DIFFERENT, report.verdict("RC1"));
        // a record without a row is also a round-trip difference; the sum of the rates does not change (the rate removed is 0.00)
        assertEquals(Verdict.DIFFERENT, report.verdict("RC3"));
        assertEquals(Verdict.EQUAL, report.verdict("RC2"));
        assertEquals(Verdict.EQUAL, report.verdict("RC4"));
        assertEquals(Verdict.EQUAL, report.verdict("RC5"));
    }

    @Test
    void rc1_brokenCountInInterestRun_isDifferentWithBothFigures() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        jdbc.update("UPDATE carddemo.interest_run SET loaded_account_count = 3");

        ReconReport report = report(BASELINE, outputDir);

        CheckLine recorded = line(report, "RC1", "count recorded at the load of account");
        assertEquals(Verdict.DIFFERENT, recorded.verdict());
        assertEquals("3", recorded.first().text());
        assertEquals("2", recorded.second().text());
        assertOnly(report, "RC1");
    }

    @Test
    void rc2_brokenSum_isDifferentWithBothFigures() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        // 5500.00 was loaded: one cent more in interest_run
        jdbc.update("UPDATE carddemo.interest_run SET loaded_acct_cash_credit_limit_sum = loaded_acct_cash_credit_limit_sum + 0.01");

        ReconReport report = report(BASELINE, outputDir);

        CheckLine sum = line(report, "RC2", "sum of ACCT-CASH-CREDIT-LIMIT, recorded at the load");
        assertEquals(Verdict.DIFFERENT, sum.verdict());
        assertEquals("5500.00", sum.first().text());
        assertEquals("5500.01", sum.second().text());
        assertEquals(Verdict.EQUAL, line(report, "RC2", "sum of ACCT-CASH-CREDIT-LIMIT, in the table now").verdict());
        assertOnly(report, "RC2");
    }

    @Test
    void rc3_brokenFillerByte_isDifferentAlthoughCountsAndSumsAreEqual() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        // one byte of the 22-byte FILLER of the first balance: no count, sum or key sees it
        jdbc.update("UPDATE carddemo.tran_cat_balance SET trancat_filler = set_byte(trancat_filler, 0, 88) WHERE trancat_acct_id = 2001");

        ReconReport report = report(BASELINE, outputDir);

        CheckLine roundTrip = line(report, "RC3", "rows of tran_cat_balance that encode to their source record");
        assertEquals(Verdict.DIFFERENT, roundTrip.verdict());
        assertEquals("2", roundTrip.first().text());
        assertEquals("1", roundTrip.second().text());
        assertTrue(roundTrip.note().contains("other differences 1 (rows that differ 1, rows without a record 0, records without a row 0)"),
                roundTrip.note());
        assertOnly(report, "RC3");
    }

    @Test
    void rc4_brokenHighestKey_isDifferentWithBothKeys() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        // the last record of DISCGRP.PS is STANDARD/05/1; in the table that row gets the type code ZZ
        assertEquals(1, jdbc.update("UPDATE carddemo.disclosure_group SET dis_tran_type_cd = 'ZZ'"
                + " WHERE dis_acct_group_id = 'STANDARD' AND dis_tran_type_cd = '05'"));

        ReconReport report = report(BASELINE, outputDir);

        CheckLine highest = line(report, "RC4", "highest key of disclosure_group");
        assertEquals(Verdict.DIFFERENT, highest.verdict());
        assertEquals("STANDARD/05/1", highest.first().text());
        assertEquals("STANDARD/ZZ/1", highest.second().text());
        assertEquals(Verdict.EQUAL, line(report, "RC4", "lowest key of disclosure_group").verdict());
        assertEquals(Verdict.DIFFERENT, report.verdict("RC4"));
        // counts and sums do not see a changed key
        assertEquals(Verdict.EQUAL, report.verdict("RC1"));
        assertEquals(Verdict.EQUAL, report.verdict("RC2"));
        assertEquals(Verdict.EQUAL, report.verdict("RC5"));
    }

    @Test
    void rc5_brokenTransactionAmount_isDifferentWithBothFigures() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        // 10.00 for account 2001 becomes 10.01: 47.01 against the exported 47.00 and against 10.00 + 37.00
        jdbc.update("UPDATE carddemo.interest_transaction SET tran_amt = tran_amt + 0.01 WHERE seq_no = 1");

        ReconReport report = report(BASELINE, outputDir);

        CheckLine sum = line(report, "RC5", "sum of TRAN-AMT");
        assertEquals(Verdict.DIFFERENT, sum.verdict());
        assertEquals("47.01", sum.first().text());
        assertEquals("47.00", sum.second().text());
        CheckLine identity = line(report, "RC5", "sum of TRAN-AMT = balance change + interest of the last account, not added (INT-Q04)");
        assertEquals(Verdict.DIFFERENT, identity.verdict());
        assertEquals("47.01", identity.first().text());
        assertEquals("47.00", identity.second().text());
        assertEquals("does not hold", report.figure("RC5").text());
        assertOnly(report, "RC5");
    }

    @Test
    void rc5_brokenTransactionCount_isDifferentWithBothFigures() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        jdbc.update("DELETE FROM carddemo.interest_transaction WHERE seq_no = 2");

        ReconReport report = report(BASELINE, outputDir);

        CheckLine written = line(report, "RC5", "transactions written");
        assertEquals(Verdict.DIFFERENT, written.verdict());
        assertEquals("1", written.first().text());
        assertEquals("2", written.second().text());
        assertOnly(report, "RC5");
    }

    @Test
    void missingExportedFiles_areNotAvailable_neverEqual() throws Exception {
        loadedAndCalculated(BASELINE);

        ReconReport report = report(BASELINE, temp.resolve("nothing-exported"));

        CheckLine written = line(report, "RC5", "transactions written");
        assertEquals(Verdict.NOT_AVAILABLE, written.verdict());
        assertEquals("2", written.first().text());
        assertEquals("NOT AVAILABLE (no exported file datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt)", written.second().text());
        assertEquals(Verdict.NOT_AVAILABLE, report.verdict("RC5"));
        assertEquals(Verdict.NOT_AVAILABLE, report.verdict());
        assertEquals(Verdict.EQUAL, report.verdict("RC1"));
    }

    @Test
    void missingUnloadedFile_isNotAvailable_neverEqual() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        Path inputDir = Files.createDirectories(temp.resolve("input"));
        for (ExtractFile file : ExtractFile.values()) {
            if (file != ExtractFile.CARD_XREFS) {
                Files.copy(Fixtures.input(BASELINE).resolve(file.fileName()), inputDir.resolve(file.fileName()));
            }
        }

        ReconReport report = report(BASELINE, inputDir, outputDir, OptionalLong.empty());

        CheckLine rows = line(report, "RC1", "records and rows of card_xref");
        assertEquals(Verdict.NOT_AVAILABLE, rows.verdict());
        assertEquals("NOT AVAILABLE (no file AWS.M2.CARDDEMO.CARDXREF.PS.txt)", rows.first().text());
        assertEquals("2", rows.second().text());
        for (String check : List.of("RC1", "RC2", "RC3", "RC4")) {
            assertEquals(Verdict.NOT_AVAILABLE, report.verdict(check), check);
        }
        assertTrue(report.profile().isEmpty());
        assertTrue(report.markdown().contains("NOT AVAILABLE: one of the four files is missing"));
    }

    @Test
    void unloadedFileWithARejectedRecord_isNotAvailable_andOnTheRejectList() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        Path inputDir = Files.createDirectories(temp.resolve("rejected"));
        for (ExtractFile file : ExtractFile.values()) {
            Files.copy(Fixtures.input(BASELINE).resolve(file.fileName()), inputDir.resolve(file.fileName()));
        }
        // record 2 of the rates: a letter in DIS-TRAN-CAT-CD (position 13 of the record)
        Path rates = inputDir.resolve(ExtractFile.DISCLOSURE_GROUPS.fileName());
        List<String> lines = new java.util.ArrayList<>(Fixtures.lines(rates));
        lines.set(1, lines.get(1).substring(0, 12) + "X" + lines.get(1).substring(13));
        Files.write(rates, lines, java.nio.charset.StandardCharsets.ISO_8859_1);

        ReconReport report = report(BASELINE, inputDir, outputDir, OptionalLong.empty());

        assertEquals(Verdict.NOT_AVAILABLE, line(report, "RC1", "records and rows of disclosure_group").verdict());
        assertEquals(1, report.rejects().size());
        assertEquals(2, report.rejects().get(0).recordNumber());
        assertEquals("DIS-TRAN-CAT-CD", report.rejects().get(0).field());
        assertTrue(report.markdown().contains("| AWS.M2.CARDDEMO.DISCGRP.PS.txt | 2 | DIS-TRAN-CAT-CD |"));
    }

    @Test
    void runThatDoesNotExist_isNotAvailable_neverEqual() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);

        ReconReport report = report(BASELINE, Fixtures.input(BASELINE), outputDir, OptionalLong.of(999_999L));

        CheckLine recorded = line(report, "RC1", "count recorded at the load of account");
        assertEquals(Verdict.NOT_AVAILABLE, recorded.verdict());
        assertEquals("NOT AVAILABLE (no row in carddemo.interest_run with run_id 999999)", recorded.first().text());
        assertEquals(Verdict.NOT_AVAILABLE, report.verdict("RC5"));
        // whether the calculation has written ACCT-CURR-BAL is not known without the run: not compared, not equal
        assertEquals(Verdict.NOT_AVAILABLE, line(report, "RC2", "sum of ACCT-CURR-BAL, in the table now").verdict());
        assertEquals(Verdict.EQUAL, report.verdict("RC3"));
        assertEquals(Verdict.NOT_AVAILABLE, report.verdict());
    }

    @Test
    void databaseThatCannotBeReached_isNotAvailable_andTheCommandDoesNotEndWithZero() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        Path reportFile = temp.resolve("unreachable.md");

        int exitCode = ReconCommand.run(new String[] {"name=" + BASELINE, "inputDir=" + Fixtures.input(BASELINE),
                "outputDir=" + outputDir, "report=" + reportFile},
                Map.of("INTEREST_DB_URL", "jdbc:postgresql://localhost:1/carddemo?connectTimeout=2"));

        assertEquals(ReconCommand.EXIT_NOT_EQUAL, exitCode);
        String text = Files.readString(reportFile);
        assertTrue(text.contains("## Reconciliation report - i01-baseline: **NOT AVAILABLE**"), text);
        assertTrue(text.contains("NOT AVAILABLE (the database cannot be reached:"), text);
        assertFalse(text.contains("| **EQUAL** |"), text);
    }

    @Test
    void command_writesTheReportOfTheLatestRun_andEndsWithZero() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        Path reportFile = temp.resolve("reports").resolve("i01.md");

        int exitCode = ReconCommand.run(new String[] {"name=" + BASELINE, "inputDir=" + Fixtures.input(BASELINE),
                "outputDir=" + outputDir, "report=" + reportFile},
                Map.of("INTEREST_DB_URL", POSTGRES.getJdbcUrl(), "INTEREST_DB_USER", POSTGRES.getUsername(),
                        "INTEREST_DB_PASSWORD", POSTGRES.getPassword()));

        assertEquals(ReconCommand.EXIT_EQUAL, exitCode);
        assertTrue(Files.readString(reportFile).contains("## Reconciliation report - i01-baseline: **EQUAL**"));
        assertEquals(ReconCommand.EXIT_USAGE, ReconCommand.run(new String[] {"name=x"}, Map.of()));
    }

    @Test
    void report_writesNothing_andItsConnectionRefusesAWrite() throws Exception {
        Path outputDir = loadedAndCalculated(BASELINE);
        String everyRow = """
                SELECT md5(concat_ws('|',
                    (SELECT string_agg(t::text, ',' ORDER BY t::text) FROM carddemo.interest_run t),
                    (SELECT string_agg(t::text, ',' ORDER BY t::text) FROM carddemo.interest_transaction t),
                    (SELECT string_agg(t::text, ',' ORDER BY t::text) FROM carddemo.account t),
                    (SELECT string_agg(t::text, ',' ORDER BY t::text) FROM carddemo.card_xref t),
                    (SELECT string_agg(t::text, ',' ORDER BY t::text) FROM carddemo.tran_cat_balance t),
                    (SELECT string_agg(t::text, ',' ORDER BY t::text) FROM carddemo.disclosure_group t)))""";
        String writes = """
                SELECT coalesce(sum(n_tup_ins + n_tup_upd + n_tup_del), 0) FROM pg_stat_xact_all_tables
                 WHERE schemaname IN ('carddemo', 'batch')""";
        String before = jdbc.queryForObject(everyRow, String.class);
        long jobExecutionsBefore = jdbc.queryForObject("SELECT count(*) FROM batch.batch_job_execution", Long.class);

        assertEquals(Verdict.EQUAL, report(BASELINE, outputDir).verdict());

        assertEquals(before, jdbc.queryForObject(everyRow, String.class));
        assertEquals(jobExecutionsBefore, jdbc.queryForObject("SELECT count(*) FROM batch.batch_job_execution", Long.class));
        try (JdbcReconTables tables = JdbcReconTables.open(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            tables.run(OptionalLong.empty());
            try (Statement statement = tables.connection().createStatement()) {
                // nothing was inserted, updated or deleted in the report's transaction
                try (var counted = statement.executeQuery(writes)) {
                    counted.next();
                    assertEquals(0, counted.getLong(1));
                }
                SQLException refused = assertThrows(SQLException.class,
                        () -> statement.executeUpdate("UPDATE carddemo.interest_run SET status = 'ABANDONED'"));
                // 25006: read_only_sql_transaction
                assertEquals("25006", refused.getSQLState());
            }
        }
        assertEquals(before, jdbc.queryForObject(everyRow, String.class));
    }
}
