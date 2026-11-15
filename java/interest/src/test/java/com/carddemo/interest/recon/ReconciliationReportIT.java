package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.ExtractFormat;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * The reconciliation report on PostgreSQL 15 in a container with the schema of FI-4 (standards
 * S7.3). Each case is loaded by the loader, calculated by interestCalcJob on its file adapters,
 * posted by the staging stand-in and exported by the exporter; then the report is made. Expected
 * figures are the reconciliation table of modernization/design/strategy-interest.md, written here
 * by hand (standards S7.4). Every check has a test in which one figure is broken on purpose.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class ReconciliationReportIT {

    private static final Path DDL = Path.of("..", "..", "db", "postgres");
    private static final String BASELINE = "i01-baseline";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("carddemo")
            .withUsername("carddemo")
            .withPassword("carddemo")
            .withCopyFileToContainer(MountableFile.forHostPath(DDL.resolve("01-transaction-type.sql")),
                    "/docker-entrypoint-initdb.d/01-transaction-type.sql")
            .withCopyFileToContainer(MountableFile.forHostPath(DDL.resolve("02-interest.sql")),
                    "/docker-entrypoint-initdb.d/02-interest.sql");

    /** strategy-interest.md, "The figures on the recorded cases". */
    private record Expected(String caseName, long accounts, long cards, long balances, long rates, String balanceBefore,
            String categoryBalanceSum, String rateSum, long transactions, String transactionAmountSum, String balanceChange,
            String notAdded, String lastAccount) {
    }

    static List<Expected> strategyTable() {
        return List.of(
                new Expected("i01-baseline", 2, 2, 2, 15, "3400.00", "3400.00", "120.50", 2, "47.00", "10.00", "37.00", "00000002002"),
                new Expected("i02-tie-half-cent", 2, 2, 2, 15, "2470.00", "2470.00", "120.50", 2, "24.69", "12.35", "12.34", "00000002004"),
                new Expected("i03-zero-rate-skipped", 1, 1, 2, 15, "700.00", "700.00", "120.50", 1, "10.00", "0.00", "10.00", "00000002005"),
                new Expected("i04-default-group-fallback", 1, 1, 1, 15, "1000.00", "1000.00", "120.50", 1, "17.50", "0.00", "17.50", "00000002006"),
                new Expected("i05-multi-category", 1, 1, 2, 15, "1500.00", "1500.00", "120.50", 2, "17.50", "0.00", "17.50", "00000002007"),
                new Expected("i06-negative-balance", 1, 1, 1, 15, "-300.00", "-300.00", "120.50", 1, "-3.00", "0.00", "-3.00", "00000002008"),
                new Expected("i07-nontie-rounding", 2, 2, 2, 15, "1778.10", "1778.10", "120.50", 2, "29.03", "15.42", "13.61", "00000002010"));
    }

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @TempDir
    Path temp;

    /** Loaded, calculated and exported: the input of the report. */
    private long stage(String caseName) throws Exception {
        Path calculated = temp.resolve(caseName).resolve("calculated");
        assertEquals(BatchStatus.COMPLETED, jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString("parmDate", Fixtures.PARM_DATE)
                .addString("runId", UUID.randomUUID().toString())
                .addString("inputDir", Fixtures.input(caseName).toString())
                .addString("outputDir", calculated.toString())
                .addString("format", "fixture")
                .toJobParameters()).getStatus());
        return new CalculatedRunStaging(jdbc, transactionManager, Clock.systemDefaultZone()).stage(Fixtures.PARM_DATE,
                Fixtures.input(caseName), ExtractFormat.FIXTURE, calculated, temp.resolve(caseName).resolve("actual"));
    }

    private ReconReport report(String caseName, long runId) {
        return report(runId, Fixtures.input(caseName));
    }

    private ReconReport report(long runId, Path inputDir) {
        return new ReconciliationReporter(jdbc, transactionManager).report("test", Optional.of(runId), inputDir, ExtractFormat.FIXTURE);
    }

    private static ReconLine line(ReconReport report, String check, String itemPart) {
        List<ReconLine> found = report.linesOf(check).stream().filter(line -> line.item().contains(itemPart)).toList();
        assertEquals(1, found.size(), check + " lines with '" + itemPart + "': " + found);
        return found.get(0);
    }

    private static void assertAllEqual(ReconReport report, String check) {
        assertFalse(report.linesOf(check).isEmpty(), check + " has no line");
        report.linesOf(check).forEach(line -> assertEquals(ReconStatus.EQUAL, line.status(), line.toString()));
    }

    /** Exactly the named lines are not EQUAL: one broken figure shows in its own check and nowhere else. */
    private static void assertOnlyDifferent(ReconReport report, ReconLine... expected) {
        List<ReconLine> notEqual = report.lines().stream().filter(line -> !line.check().equals("RC6"))
                .filter(line -> line.status() != ReconStatus.EQUAL).toList();
        assertEquals(List.of(expected), notEqual);
        for (ReconLine line : expected) {
            assertEquals(ReconStatus.DIFFERENT, line.status(), line.toString());
        }
        assertFalse(report.reconciled());
    }

    /** Every row of every table the report reads, as text. */
    private List<String> databaseContents() {
        List<String> rows = new ArrayList<>();
        for (String table : List.of("account", "card_xref", "tran_cat_balance", "disclosure_group", "interest_run", "interest_transaction")) {
            rows.addAll(jdbc.queryForList("select '" + table + " ' || t::text from carddemo." + table + " t order by 1", String.class));
        }
        return rows;
    }

    // ---- all seven cases: the figures of the strategy's reconciliation table

    @ParameterizedTest
    @MethodSource("strategyTable")
    void everyCaseReconciles_withTheFiguresOfTheStrategyTable(Expected expected) throws Exception {
        ReconReport report = report(expected.caseName(), stage(expected.caseName()));

        for (String check : List.of("RC1", "RC2", "RC3", "RC4", "RC5")) {
            assertAllEqual(report, check);
        }
        assertTrue(report.reconciled());
        assertEquals(String.valueOf(expected.accounts()), line(report, "RC1", "account:").leftText());
        assertEquals(String.valueOf(expected.accounts()), line(report, "RC1", "account:").rightText());
        assertEquals(String.valueOf(expected.cards()), line(report, "RC1", "card_xref:").rightText());
        assertEquals(String.valueOf(expected.balances()), line(report, "RC1", "tran_cat_balance:").rightText());
        assertEquals(String.valueOf(expected.rates()), line(report, "RC1", "disclosure_group:").rightText());
        assertEquals(expected.balanceBefore(), line(report, "RC2", "ACCT-CURR-BAL").leftText());
        assertEquals(expected.balanceBefore(), line(report, "RC2", "ACCT-CURR-BAL").rightText());
        assertEquals(expected.categoryBalanceSum(), line(report, "RC2", "TRAN-CAT-BAL").rightText());
        assertEquals(expected.rateSum(), line(report, "RC2", "DIS-INT-RATE").rightText());
        assertEquals(Optional.of(expected.transactions()), report.rc5().transactions());
        assertEquals(Optional.of(new BigDecimal(expected.transactionAmountSum())), report.rc5().transactionAmountSum());
        assertEquals(Optional.of(new BigDecimal(expected.balanceChange())), report.rc5().balanceChange());
        assertEquals(Optional.of(new BigDecimal(expected.notAdded())), report.rc5().interestNotAdded());
        assertEquals(Optional.of(expected.lastAccount()), report.rc5().lastAccount());
        assertEquals(expected.transactionAmountSum(), line(report, "RC5", "sum of TRAN-AMT").leftText());
        assertEquals(expected.transactionAmountSum(), line(report, "RC5", "sum of TRAN-AMT").rightText());
        // The export of the tables is the recorded legacy output, byte for byte.
        for (String file : List.of("datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt", "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt",
                "sysout/INTCALC.STEP15.txt", "rc.json")) {
            assertEquals(Fixtures.text(Fixtures.expected(expected.caseName()).resolve(file)),
                    Fixtures.text(temp.resolve(expected.caseName()).resolve("actual").resolve(file)), file);
        }
    }

    @Test
    void i01Baseline_theReportStatesTheAcceptanceFigures() throws Exception {
        String markdown = report(BASELINE, stage(BASELINE)).toMarkdown();

        assertTrue(markdown.contains("- transactions written: 2, summing 47.00"), markdown);
        assertTrue(markdown.contains("minus as loaded): 10.00"), markdown);
        assertTrue(markdown.contains("(INT-Q04): 37.00 (account 00000002002)"), markdown);
        assertTrue(markdown.contains("**RECONCILED**"), markdown);
        for (String check : List.of("| RC1 |", "| RC2 |", "| RC3 |", "| RC4 |", "| RC5 |", "| RC6 |")) {
            assertTrue(markdown.contains(check), check);
        }
    }

    @Test
    void theProfileOfTl4IsPartOfTheReport_allZeroOnTheRecordedCase() throws Exception {
        ReconReport report = report(BASELINE, stage(BASELINE));

        assertEquals(List.of("accounts with several cards", "accounts with mixed type codes (letters and digits)", "minus zero amounts",
                "F-signed amounts", "fillers that are not blank", "accounts that are not active",
                "balances without a rate and without a DEFAULT rate"), report.profile().stream().map(ProfileLine::finding).toList());
        report.profile().forEach(line -> assertEquals(Optional.of(0L), line.count(), line.finding()));
        assertTrue(report.toMarkdown().contains("## Profile of the load (TL4)"));
    }

    @Test
    void theProfileCountsWhatTheTablesHold() throws Exception {
        long runId = stage(BASELINE);
        // Synthetic rows (standards S8.5): a second card, a letter type code without a rate, an inactive account, a filler.
        jdbc.update("insert into carddemo.card_xref (xref_card_num, xref_cust_id, xref_acct_id, xref_filler)"
                + " select '4000000000009999', xref_cust_id, xref_acct_id, xref_filler from carddemo.card_xref where xref_acct_id = 2001");
        jdbc.update("insert into carddemo.tran_cat_balance (trancat_acct_id, trancat_type_cd, trancat_cd, tran_cat_bal, trancat_filler)"
                + " select trancat_acct_id, 'ZZ', 9999, 1.00, trancat_filler from carddemo.tran_cat_balance where trancat_acct_id = 2001");
        jdbc.update("update carddemo.account set acct_active_status = 'N', acct_filler = set_byte(acct_filler, 0, 88) where acct_id = 2002");
        jdbc.update("update carddemo.interest_run set loaded_g02_difference_count = 3, loaded_g03_difference_count = 4 where run_id = ?", runId);

        List<Optional<Long>> counts = report(BASELINE, runId).profile().stream().map(ProfileLine::count).toList();

        assertEquals(List.of(Optional.of(1L), Optional.of(1L), Optional.of(4L), Optional.of(3L), Optional.of(1L), Optional.of(1L),
                Optional.of(1L)), counts);
    }

    // ---- one deliberately broken figure per check

    @Test
    void rc1_aRowMissingFromATableShowsAsDifferentWithBothFigures() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("delete from carddemo.disclosure_group where dis_acct_group_id = 'GOLD' and dis_tran_type_cd = '03'");

        ReconLine rc1 = line(report(BASELINE, runId), "RC1", "disclosure_group:");

        assertEquals(ReconStatus.DIFFERENT, rc1.status());
        assertEquals("15", rc1.leftText());
        assertEquals("14", rc1.rightText());
    }

    @Test
    void rc2_aSumOneCentOffShowsAsDifferentAndNothingElseDoes() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("update carddemo.interest_run set loaded_tran_cat_bal_sum = loaded_tran_cat_bal_sum + 0.01 where run_id = ?", runId);

        ReconReport report = report(BASELINE, runId);

        ReconLine rc2 = line(report, "RC2", "TRAN-CAT-BAL");
        assertEquals("3400.00", rc2.leftText());
        assertEquals("3400.01", rc2.rightText());
        assertOnlyDifferent(report, rc2);
    }

    @Test
    void rc2_aHashTotalThatIsOffShowsAsDifferent() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("update carddemo.interest_run set loaded_xref_acct_id_sum = 4004 where run_id = ?", runId);

        ReconReport report = report(BASELINE, runId);

        assertOnlyDifferent(report, line(report, "RC2", "XREF-ACCT-ID"));
    }

    @Test
    void rc3_aRowThatNoLongerEncodesToItsRecordShowsAsDifferent() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("update carddemo.account set acct_group_id = 'SILVER' where acct_id = 2001");

        ReconReport report = report(BASELINE, runId);

        ReconLine rc3 = line(report, "RC3", "account:");
        assertEquals("0", rc3.leftText());
        assertEquals("2", rc3.rightText());
        assertOnlyDifferent(report, rc3);
    }

    @Test
    void rc3_aGapCountThatIsOffShowsAsDifferent() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("update carddemo.interest_run set loaded_g02_difference_count = 1 where run_id = ?", runId);

        ReconReport report = report(BASELINE, runId);

        assertOnlyDifferent(report, line(report, "RC3", "G02"));
    }

    @Test
    void rc3_fSignedAndMinusZeroAmountsAreCountedPerGap_notAsOtherDifferences() throws Exception {
        // 3400.00 summed over two accounts; one credit limit F-signed (plain digit), one cycle debit minus zero.
        Path input = temp.resolve("signs");
        Files.createDirectories(input);
        for (ExtractFile file : ExtractFile.values()) {
            String name = file.fileName(ExtractFormat.FIXTURE);
            List<String> lines = new ArrayList<>(Fixtures.lines(Fixtures.input(BASELINE).resolve(name)));
            if (file == ExtractFile.ACCOUNT) {
                String first = lines.get(0);
                assertEquals('{', first.charAt(35));
                assertEquals('{', first.charAt(101));
                lines.set(0, first.substring(0, 35) + '0' + first.substring(36, 101) + '}' + first.substring(102));
            }
            Files.write(input.resolve(name), lines, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        Path calculated = temp.resolve("signs-calculated");
        assertEquals(BatchStatus.COMPLETED, jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString("parmDate", Fixtures.PARM_DATE).addString("runId", UUID.randomUUID().toString())
                .addString("inputDir", input.toString()).addString("outputDir", calculated.toString())
                .addString("format", "fixture").toJobParameters()).getStatus());
        long runId = new CalculatedRunStaging(jdbc, transactionManager, Clock.systemDefaultZone()).stage(Fixtures.PARM_DATE, input,
                ExtractFormat.FIXTURE, calculated, temp.resolve("signs-actual"));

        ReconReport report = report(runId, input);

        assertTrue(report.reconciled(), report.toMarkdown());
        assertEquals("1", line(report, "RC3", "G02").leftText());
        assertEquals("1", line(report, "RC3", "G02").rightText());
        assertEquals("1", line(report, "RC3", "G03").leftText());
        assertEquals("1", line(report, "RC3", "G03").rightText());
        assertEquals("0", line(report, "RC3", "account:").rightText());
        assertEquals(Optional.of(1L), report.profile().get(2).count());
        assertEquals(Optional.of(1L), report.profile().get(3).count());
    }

    @Test
    void rc4_aTableWithoutItsHighestKeyShowsAsDifferent() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("delete from carddemo.card_xref where xref_card_num = '4000000000002002'");

        ReconReport report = report(BASELINE, runId);

        ReconLine highest = line(report, "RC4", "card_xref: highest key");
        assertEquals(ReconStatus.DIFFERENT, highest.status());
        assertEquals("4000000000002002", highest.leftText());
        assertEquals("4000000000002001", highest.rightText());
        assertEquals(ReconStatus.EQUAL, line(report, "RC4", "card_xref: lowest key").status());
    }

    @Test
    void rc5_aTransactionMissingFromTheTableShowsAsDifferentInCountAndSum() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("delete from carddemo.interest_transaction where run_id = ? and seq_no = 2", runId);

        ReconReport report = report(BASELINE, runId);

        ReconLine count = line(report, "RC5", "exported SYSTRAN");
        assertEquals("1", count.leftText());
        assertEquals("2", count.rightText());
        ReconLine sum = line(report, "RC5", "sum of TRAN-AMT");
        assertEquals("10.00", sum.leftText());
        assertEquals("47.00", sum.rightText());
        assertOnlyDifferent(report, count, line(report, "RC5", "calculation step"), sum);
    }

    @Test
    void rc5_anAmountOneCentOffShowsAsDifferent() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("update carddemo.interest_transaction set tran_amt = tran_amt + 0.01 where run_id = ? and seq_no = 1", runId);

        ReconReport report = report(BASELINE, runId);

        ReconLine sum = line(report, "RC5", "sum of TRAN-AMT");
        assertEquals("47.01", sum.leftText());
        assertEquals("47.00", sum.rightText());
        assertOnlyDifferent(report, sum);
    }

    @Test
    void rc5_theLastAccountUpdatedAfterAll_theBalanceChangeNoLongerAddsUp() throws Exception {
        long runId = stage(BASELINE);
        // INT-Q04 broken on purpose: the 37.00 are added to account 2002 although the run says they were not.
        jdbc.update("update carddemo.account set acct_curr_bal = acct_curr_bal + 37.00 where acct_id = 2002");

        ReconReport report = report(BASELINE, runId);

        ReconLine sum = line(report, "RC5", "sum of TRAN-AMT");
        assertEquals("47.00", sum.leftText());
        assertEquals("84.00", sum.rightText());
        assertOnlyDifferent(report, sum);
    }

    // ---- errors: NOT AVAILABLE, never EQUAL

    @Test
    void aFigureTheRunDidNotRecordIsNotAvailable() throws Exception {
        long runId = stage(BASELINE);
        jdbc.update("update carddemo.interest_run set written_interest_not_added_sum = null, loaded_dis_int_rate_sum = null where run_id = ?", runId);

        ReconReport report = report(BASELINE, runId);

        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC5", "sum of TRAN-AMT").status());
        assertEquals("NOT AVAILABLE", line(report, "RC5", "sum of TRAN-AMT").rightText());
        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC2", "DIS-INT-RATE").status());
        assertFalse(report.reconciled());
        assertTrue(report.toMarkdown().contains("**NOT RECONCILED**"));
        assertTrue(report.toMarkdown().contains("(INT-Q04): NOT AVAILABLE"));
    }

    @Test
    void aFileThatIsMissingOrUndecodableMakesItsLinesNotAvailable() throws Exception {
        long runId = stage(BASELINE);
        Path input = temp.resolve("broken-input");
        Files.createDirectories(input);
        for (ExtractFile file : List.of(ExtractFile.ACCOUNT, ExtractFile.TRAN_CAT_BALANCE, ExtractFile.DISCLOSURE_GROUP)) {
            String name = file.fileName(ExtractFormat.FIXTURE);
            List<String> lines = new ArrayList<>(Fixtures.lines(Fixtures.input(BASELINE).resolve(name)));
            if (file == ExtractFile.TRAN_CAT_BALANCE) {
                lines.set(0, "X" + lines.get(0).substring(1));
            }
            Files.write(input.resolve(name), lines, java.nio.charset.StandardCharsets.ISO_8859_1);
        }

        ReconReport report = report(runId, input);

        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC1", "card_xref:").status());
        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC1", "tran_cat_balance:").status());
        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC2", "sum of TRAN-CAT-BAL").status());
        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC3", "card_xref:").status());
        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC3", "G02").status());
        assertEquals(ReconStatus.NOT_AVAILABLE, line(report, "RC4", "card_xref: highest key").status());
        assertEquals(ReconStatus.EQUAL, line(report, "RC1", "account:").status());
        assertEquals(0, report.lines().stream().filter(line -> line.left().isEmpty() || line.right().isEmpty())
                .filter(line -> line.status() == ReconStatus.EQUAL).count());
        assertFalse(report.reconciled());
    }

    @Test
    void aRunThatIsOnlyLoadedHasNoRc5_andARunThatDoesNotExistHasNoFigures() throws Exception {
        stage(BASELINE);
        long loadedOnly = jdbc.queryForObject("insert into carddemo.interest_run (parm_date, status, loaded_acct_curr_bal_sum)"
                + " values ('2022071800', 'LOADED', 3400.00) returning run_id", Long.class);

        ReconReport loaded = report(BASELINE, loadedOnly);
        loaded.linesOf("RC5").forEach(line -> assertEquals(ReconStatus.NOT_AVAILABLE, line.status(), line.toString()));
        assertEquals(Optional.empty(), loaded.rc5().balanceChange());

        ReconReport unknown = report(BASELINE, 999_999L);
        unknown.linesOf("RC2").forEach(line -> assertEquals(ReconStatus.NOT_AVAILABLE, line.status(), line.toString()));
        unknown.linesOf("RC5").forEach(line -> assertEquals(ReconStatus.NOT_AVAILABLE, line.status(), line.toString()));
        assertFalse(unknown.reconciled());
    }

    @Test
    void rc6_cannotBeShownOffHost_andTheReportSaysSo() throws Exception {
        ReconReport report = report(BASELINE, stage(BASELINE));

        assertEquals(1, report.linesOf("RC6").size());
        assertEquals(ReconStatus.NOT_AVAILABLE, report.linesOf("RC6").get(0).status());
        assertTrue(report.toMarkdown().contains("RC6 cannot be shown off-host"));
        assertTrue(report.reconciled());
    }

    // ---- database assertions: the report reads, it writes nothing

    @Test
    void theReportWritesNothing() throws Exception {
        long runId = stage(BASELINE);
        List<String> before = databaseContents();

        report(BASELINE, runId);
        report(runId, temp.resolve("no-such-directory"));

        assertEquals(before, databaseContents());
    }

    @Test
    void theReportsTransactionIsReadOnly_aWriteInItIsRefusedByTheDatabase() throws Exception {
        long runId = stage(BASELINE);
        TransactionTemplate asTheReport = new TransactionTemplate(transactionManager);
        asTheReport.setReadOnly(true);

        DataAccessException refused = assertThrows(DataAccessException.class, () -> asTheReport.executeWithoutResult(status -> {
            jdbc.execute("SET TRANSACTION READ ONLY");
            jdbc.update("update carddemo.interest_run set status = 'CHANGED' where run_id = ?", runId);
        }));

        assertTrue(refused.getMessage().contains("read-only transaction"), refused.getMessage());
        assertEquals("EXPORTED", jdbc.queryForObject("select status from carddemo.interest_run where run_id = ?", String.class, runId));
    }
}
