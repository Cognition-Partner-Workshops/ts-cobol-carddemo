package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.export.DatasetExporter;
import com.carddemo.interest.load.ExtractFormat;
import com.carddemo.interest.load.ExtractLoader;

/**
 * INT-T10 on PostgreSQL 15 in a container (standards S7.3): the report of a loaded, calculated and
 * exported case, read from interest_run and the tables. The load is ticket 05's loader, the export
 * ticket 07's exporter; the calculation on the tables (ticket 06) is stood in for by SQL that
 * inserts the recorded transactions and sets the account columns the calculation updates.
 * The seven reports are kept under target/recon/ as the ticket's final-revision evidence.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class ReconReporterIT {

    private static final String BASELINE = "i01-baseline";
    private static final String SYSTRAN = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    private static final String ACCTDATA = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    private static final Path REPORTS = Path.of("target", "recon");
    private static final List<String> ALL_TABLES = List.of("account", "card_xref", "tran_cat_balance", "disclosure_group",
            "interest_transaction", "interest_run");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("carddemo")
            .withUsername("carddemo")
            .withPassword("carddemo")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", "01-transaction-type.sql")),
                    "/docker-entrypoint-initdb.d/01-transaction-type.sql")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", "02-interest.sql")),
                    "/docker-entrypoint-initdb.d/02-interest.sql");

    @Autowired
    private ReconReporter reporter;
    @Autowired
    private ExtractLoader loader;
    @Autowired
    private DatasetExporter exporter;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    private static List<DecodedRecord> recorded(String caseName, String dataset, RecordLayout layout) {
        List<DecodedRecord> records = new ArrayList<>();
        RecordSource source = RecordSource.built(dataset);
        for (String line : Fixtures.lines(Fixtures.expected(caseName).resolve(dataset + ".txt"))) {
            records.add(layout.decode(RecordImage.bytes(RecordImage.fromLine(line, layout, source)), CodePage.FIXTURE, source));
        }
        return records;
    }

    private long load(String caseName) {
        return loader.load(Fixtures.input(caseName), ExtractFormat.FIXTURE, Fixtures.PARM_DATE,
                temp.resolve("rejects-" + UUID.randomUUID() + ".txt")).runId();
    }

    /** What the calculation leaves in the tables, by SQL from the recorded files of the case. */
    private void calculate(String caseName, long runId) {
        RecordLayout layout = CardDemoLayouts.TRANSACTION;
        List<String> columns = layout.fields().stream()
                .map(field -> field.kind() == FieldKind.FILLER ? "tran_filler" : field.name().toLowerCase(Locale.ROOT).replace('-', '_')).toList();
        String insert = "INSERT INTO carddemo.interest_transaction (run_id, seq_no, " + String.join(", ", columns) + ") VALUES (?, ?"
                + ", ?".repeat(columns.size()) + ")";
        List<DecodedRecord> transactions = recorded(caseName, SYSTRAN, layout);
        for (int i = 0; i < transactions.size(); i++) {
            List<Object> values = new ArrayList<>(List.of(runId, i + 1));
            for (LayoutField field : layout.fields()) {
                values.add(switch (field.kind()) {
                    case FILLER -> transactions.get(i).filler(field.name());
                    case TEXT -> transactions.get(i).text(field.name());
                    default -> transactions.get(i).number(field.name());
                });
            }
            jdbc.update(insert, values.toArray());
        }
        for (DecodedRecord account : recorded(caseName, ACCTDATA, CardDemoLayouts.ACCOUNT)) {
            jdbc.update("UPDATE carddemo.account SET acct_curr_bal = ?, acct_curr_cyc_credit = ?, acct_curr_cyc_debit = ? WHERE acct_id = ?",
                    account.number("ACCT-CURR-BAL"), account.number("ACCT-CURR-CYC-CREDIT"), account.number("ACCT-CURR-CYC-DEBIT"),
                    account.number("ACCT-ID"));
        }
        jdbc.update("""
                UPDATE carddemo.interest_run
                   SET status = 'CALCULATED',
                       written_transaction_count = (SELECT COUNT(*) FROM carddemo.interest_transaction WHERE run_id = ?),
                       written_tran_amt_sum = (SELECT COALESCE(SUM(tran_amt), 0) FROM carddemo.interest_transaction WHERE run_id = ?),
                       written_acct_curr_bal_sum = (SELECT SUM(acct_curr_bal) FROM carddemo.account)
                 WHERE run_id = ?""", runId, runId, runId);
    }

    /** A loaded, calculated and exported case; returns its run id. */
    private long finishedCase(String caseName, Path out) {
        long runId = load(caseName);
        calculate(caseName, runId);
        exporter.export(runId, out, ExtractFormat.FIXTURE);
        return runId;
    }

    private ReconReport report(String caseName, Path out) {
        return reporter.report(caseName, Fixtures.input(caseName), out, ExtractFormat.FIXTURE, OptionalLong.empty());
    }

    private List<String> rows() {
        List<String> rows = new ArrayList<>();
        for (String table : ALL_TABLES) {
            rows.addAll(jdbc.queryForList("SELECT '" + table + " ' || t::text FROM carddemo." + table + " t ORDER BY 1", String.class));
        }
        return rows;
    }

    private static List<ReconLine> lines(ReconReport report, String check, Verdict verdict) {
        return report.lines().stream().filter(line -> line.check().equals(check) && line.verdict() == verdict).toList();
    }

    @ParameterizedTest
    @CsvSource({
            "i01-baseline, 2, 2, 2, 15, 3400.00, 3400.00, 120.50, 2, 47.00, 3410.00, 10.00, 37.00",
            "i02-tie-half-cent, 2, 2, 2, 15, 2470.00, 2470.00, 120.50, 2, 24.69, 2482.35, 12.35, 12.34",
            "i03-zero-rate-skipped, 1, 1, 2, 15, 700.00, 700.00, 120.50, 1, 10.00, 700.00, 0.00, 10.00",
            "i04-default-group-fallback, 1, 1, 1, 15, 1000.00, 1000.00, 120.50, 1, 17.50, 1000.00, 0.00, 17.50",
            "i05-multi-category, 1, 1, 2, 15, 1500.00, 1500.00, 120.50, 2, 17.50, 1500.00, 0.00, 17.50",
            "i06-negative-balance, 1, 1, 1, 15, -300.00, -300.00, 120.50, 1, -3.00, -300.00, 0.00, -3.00",
            "i07-nontie-rounding, 2, 2, 2, 15, 1778.10, 1778.10, 120.50, 2, 29.03, 1793.52, 15.42, 13.61"})
    void everyCase_reportFromTheDatabase_figuresOfTheStrategyTable(String caseName, String accounts, String cards, String balances,
            String rates, String balanceBefore, String categoryBalance, String rate, String transactions, String transactionSum,
            String balanceAfter, String change, String lastAccountInterest) throws IOException {
        Path out = REPORTS.resolve(caseName).resolve("actual");
        long runId = finishedCase(caseName, out);
        List<String> before = rows();

        ReconReport report = report(caseName, out);

        assertEquals(before, rows(), "the report writes nothing");
        String markdown = report.markdown();
        Files.createDirectories(REPORTS);
        Files.writeString(REPORTS.resolve(caseName + ".md"), markdown, StandardCharsets.UTF_8);
        System.out.println("recon " + caseName + ": " + report.count(Verdict.EQUAL) + " EQUAL, " + report.count(Verdict.DIFFERENT)
                + " DIFFERENT, " + report.count(Verdict.NOT_AVAILABLE) + " NOT AVAILABLE | " + report.header().get(1));

        assertEquals(0, report.count(Verdict.DIFFERENT), markdown);
        assertEquals(List.of("RC6"), report.lines().stream().filter(line -> line.verdict() == Verdict.NOT_AVAILABLE).map(ReconLine::check).toList());
        assertEquals("Run: interest_run " + runId + ", status EXPORTED, PARM date " + Fixtures.PARM_DATE, report.header().get(0));
        assertEquals("RC5 in words: " + transactions + " transaction(s) summing " + transactionSum + "; sum of ACCT-CURR-BAL " + balanceBefore
                + " before and " + balanceAfter + " after, a balance change of " + change + "; interest of the last account "
                + lastAccountInterest + ", not added to the last account (INT-Q04).", report.header().get(1));
        for (String line : List.of(
                "= rows in account | unloaded file: " + accounts + " | table: " + accounts + " | **EQUAL**",
                "= rows in card_xref | unloaded file: " + cards + " | table: " + cards + " | **EQUAL**",
                "= rows in tran_cat_balance | unloaded file: " + balances + " | table: " + balances + " | **EQUAL**",
                "= rows in disclosure_group | unloaded file: " + rates + " | table: " + rates + " | **EQUAL**",
                "| sum of ACCT-CURR-BAL | unloaded file: " + balanceBefore + " | interest_run: " + balanceBefore + " | **EQUAL**",
                "| sum of TRAN-CAT-BAL | unloaded file: " + categoryBalance + " | interest_run: " + categoryBalance + " | **EQUAL**",
                "| sum of DIS-INT-RATE | unloaded file: " + rate + " | interest_run: " + rate + " | **EQUAL**",
                "SYSTRAN generation | table: " + transactions + " | exported file: " + transactions + " | **EQUAL**",
                "SYSTRAN generation | table: " + transactionSum + " | exported file: " + transactionSum + " | **EQUAL**",
                "| exported file: " + transactionSum + " | balance change + interest of the last account: " + transactionSum
                        + " | **EQUAL** | " + change + " + " + lastAccountInterest + " |",
                "| interest_run: " + balanceAfter + " | exported file: " + balanceAfter + " | **EQUAL**",
                "Nothing found.")) {
            assertTrue(markdown.contains(line), line + "\n" + markdown);
        }
    }

    @Test
    void loadedOnly_rc1ToRc4Equal_rc5OnTheTablesNotAvailable() {
        long runId = load(BASELINE);

        ReconReport report = reporter.report(BASELINE, Fixtures.input(BASELINE), Fixtures.expected(BASELINE), ExtractFormat.FIXTURE, OptionalLong.of(runId));

        assertEquals(0, report.count(Verdict.DIFFERENT), report.markdown());
        assertEquals(30, lines(report, "RC1", Verdict.EQUAL).size() + lines(report, "RC2", Verdict.EQUAL).size()
                + lines(report, "RC3", Verdict.EQUAL).size() + lines(report, "RC4", Verdict.EQUAL).size());
        assertEquals(5, lines(report, "RC5", Verdict.NOT_AVAILABLE).size());
    }

    @Test
    void brokenFigures_oneForEachCheck_areDifferentWithBothFigures() {
        Path out = temp.resolve("out");
        long runId = finishedCase(BASELINE, out);
        assertEquals(0, report(BASELINE, out).count(Verdict.DIFFERENT));

        // RC1 and RC2: what the load recorded.
        jdbc.update("UPDATE carddemo.interest_run SET loaded_card_xref_count = 3, loaded_dis_int_rate_sum = 120.51 WHERE run_id = ?", runId);
        // RC3: a column the calculation does not write.
        jdbc.update("UPDATE carddemo.account SET acct_credit_limit = acct_credit_limit + 1 WHERE acct_id = 2001");
        // RC4: the highest key of the table is no longer the last record of the file.
        jdbc.update("DELETE FROM carddemo.tran_cat_balance WHERE trancat_acct_id = 2002");
        // RC5: the table against the exported file, and what the calculation recorded.
        jdbc.update("UPDATE carddemo.interest_transaction SET tran_amt = tran_amt + 0.01 WHERE run_id = ? AND seq_no = 1", runId);
        jdbc.update("UPDATE carddemo.interest_run SET written_transaction_count = 5 WHERE run_id = ?", runId);

        ReconReport report = report(BASELINE, out);
        String markdown = report.markdown();

        assertTrue(markdown.contains("unloaded file: 2 | interest_run: 3 | **DIFFERENT**"), markdown);
        assertTrue(markdown.contains("| sum of DIS-INT-RATE | unloaded file: 120.50 | interest_run: 120.51 | **DIFFERENT**"), markdown);
        assertEquals(List.of("every row of account, encoded again, equals its record of AWS.M2.CARDDEMO.ACCTDATA.PS.txt",
                        "every row of tran_cat_balance, encoded again, equals its record of AWS.M2.CARDDEMO.TCATBALF.PS.txt"),
                lines(report, "RC3", Verdict.DIFFERENT).stream().map(ReconLine::what).toList());
        assertEquals(1, lines(report, "RC4", Verdict.DIFFERENT).size());
        assertTrue(markdown.contains("first to last record (key ends): ...2001/01/0001 to ...2002/01/0001 "
                + "| lowest to highest key (key ends): ...2001/01/0001 to ...2001/01/0001 | **DIFFERENT**"), markdown);
        assertTrue(markdown.contains("| table: 47.01 | exported file: 47.00 | **DIFFERENT**"), markdown);
        assertTrue(markdown.contains("| interest_run: 5 | exported file: 2 | **DIFFERENT**"), markdown);
        assertFalse(lines(report, "RC1", Verdict.DIFFERENT).isEmpty());
        assertFalse(lines(report, "RC2", Verdict.DIFFERENT).isEmpty());
        assertFalse(lines(report, "RC5", Verdict.DIFFERENT).isEmpty());
    }

    @Test
    void unknownRun_isNotAvailable_notEqual() {
        load(BASELINE);

        ReconReport report = reporter.report(BASELINE, Fixtures.input(BASELINE), Fixtures.expected(BASELINE), ExtractFormat.FIXTURE, OptionalLong.of(-1));

        assertEquals("Run: no row in interest_run", report.header().get(0));
        assertEquals(0, lines(report, "RC1", Verdict.EQUAL).size());
        assertEquals(8, lines(report, "RC1", Verdict.NOT_AVAILABLE).size());
    }

    @Test
    void failedLoad_isReportedAsNotAvailable() throws IOException {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        try (var files = Files.list(Fixtures.input(BASELINE))) {
            for (Path file : files.toList()) {
                Files.copy(file, input.resolve(file.getFileName()));
            }
        }
        Path balances = input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt");
        Files.writeString(balances, Fixtures.text(balances).replace("0000024000{", "00000ABCDE{"), StandardCharsets.ISO_8859_1);
        try {
            loader.load(input, ExtractFormat.FIXTURE, Fixtures.PARM_DATE, temp.resolve("rejects.txt"));
        } catch (RuntimeException expected) {
            // the load fails; its run is FAILED
        }

        ReconReport report = reporter.report(BASELINE, input, Fixtures.expected(BASELINE), ExtractFormat.FIXTURE, OptionalLong.empty());

        assertTrue(report.header().get(0).contains("status FAILED"), report.header().get(0));
        assertEquals(0, lines(report, "RC1", Verdict.EQUAL).size());
        assertEquals(0, lines(report, "RC3", Verdict.EQUAL).size());
        assertEquals(0, report.count(Verdict.DIFFERENT));
    }
}
