package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.ReplicaTableWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * loadExtractsStep on PostgreSQL 15 in a container with the schema of FI-4 (standards S7.3):
 * postgres:15 with db/postgres/01-transaction-type.sql and 02-interest.sql as init scripts, as the
 * db service of docker-compose.yml has them. Expected figures are the reconciliation table of
 * modernization/design/strategy-interest.md, written here by hand (standards S7.4).
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class LoadExtractsStepIT {

    private static final Path DDL = Path.of("..", "..", "db", "postgres");

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

    /** strategy-interest.md, "The figures on the recorded cases": accounts, cards, balances, rates, three sums. */
    private record Expected(String caseName, long accounts, long cards, long balances, long rates, String currentBalanceSum,
            String categoryBalanceSum, String rateSum) {
    }

    private static final List<Expected> STRATEGY_TABLE = List.of(
            new Expected("i01-baseline", 2, 2, 2, 15, "3400.00", "3400.00", "120.50"),
            new Expected("i02-tie-half-cent", 2, 2, 2, 15, "2470.00", "2470.00", "120.50"),
            new Expected("i03-zero-rate-skipped", 1, 1, 2, 15, "700.00", "700.00", "120.50"),
            new Expected("i04-default-group-fallback", 1, 1, 1, 15, "1000.00", "1000.00", "120.50"),
            new Expected("i05-multi-category", 1, 1, 2, 15, "1500.00", "1500.00", "120.50"),
            new Expected("i06-negative-balance", 1, 1, 1, 15, "-300.00", "-300.00", "120.50"),
            new Expected("i07-nontie-rounding", 2, 2, 2, 15, "1778.10", "1778.10", "120.50"));

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private Step loadExtractsStep;
    @Autowired
    private ExtractLoader loader;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    private Path rejectFile() {
        return temp.resolve("rejects").resolve(LoadExtractsStepConfig.REJECT_FILE_NAME);
    }

    private LoadReport load(Path inputDir) {
        return loader.load(new LoadRequest(Fixtures.PARM_DATE, inputDir, ExtractFormat.FIXTURE, rejectFile()));
    }

    private JobExecution launchStep(Path inputDir) throws Exception {
        Job loadOnly = new JobBuilder("loadExtractsStepIT", jobRepository).start(loadExtractsStep).build();
        return jobLauncher.run(loadOnly, new JobParametersBuilder()
                .addString("parmDate", Fixtures.PARM_DATE)
                .addString("runId", UUID.randomUUID().toString())
                .addString("inputDir", inputDir.toString())
                .addString("outputDir", temp.resolve("actual").toString())
                .addString("format", "fixture")
                .toJobParameters());
    }

    private long count(String table) {
        return jdbc.queryForObject("select count(*) from carddemo." + table, Long.class);
    }

    private BigDecimal sum(String table, String column) {
        return jdbc.queryForObject("select sum(" + column + ") from carddemo." + table, BigDecimal.class);
    }

    private Map<String, Object> run(long runId) {
        return jdbc.queryForMap("select * from carddemo.interest_run where run_id = ?", runId);
    }

    private long lastRunId() {
        return jdbc.queryForObject("select max(run_id) from carddemo.interest_run", Long.class);
    }

    /** Every row of the four replica tables as text, in a fixed order: what the tables hold. */
    private List<String> tableContents() {
        List<String> rows = new ArrayList<>();
        for (ExtractFile file : ExtractFile.values()) {
            rows.addAll(jdbc.queryForList("select '" + file.table() + " ' || t::text from " + file.qualifiedTable()
                    + " t order by 1", String.class));
        }
        return rows;
    }

    private static void assertAmount(String expected, Object actual, String what) {
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) actual), what + ": expected " + expected + " but was " + actual);
    }

    @Test
    void i01Baseline_countsAndSumsInTheTablesAndInInterestRun() throws Exception {
        JobExecution execution = launchStep(Fixtures.input("i01-baseline"));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, count("account"));
        assertEquals(2, count("card_xref"));
        assertEquals(2, count("tran_cat_balance"));
        assertEquals(15, count("disclosure_group"));
        // 1000.00 + 2400.00 (ACCTDATA and TCATBALF); 21.00+27.00+12.00+18.00+18.50+24.00 and nine zero rates (DISCGRP).
        assertAmount("3400.00", sum("account", "acct_curr_bal"), "SUM(acct_curr_bal)");
        assertAmount("3400.00", sum("tran_cat_balance", "tran_cat_bal"), "SUM(tran_cat_bal)");
        assertAmount("120.50", sum("disclosure_group", "dis_int_rate"), "SUM(dis_int_rate)");

        long runId = execution.getExecutionContext().getLong(LoadExtractsStepConfig.RUN_ID_KEY);
        assertEquals(lastRunId(), runId);
        Map<String, Object> run = run(runId);
        assertEquals("LOADED", run.get("status"));
        assertEquals(Fixtures.PARM_DATE, run.get("parm_date"));
        assertEquals(2L, run.get("loaded_account_count"));
        assertEquals(2L, run.get("loaded_card_xref_count"));
        assertEquals(2L, run.get("loaded_tran_cat_balance_count"));
        assertEquals(15L, run.get("loaded_disclosure_group_count"));
        assertAmount("3400.00", run.get("loaded_acct_curr_bal_sum"), "loaded_acct_curr_bal_sum");
        assertAmount("3400.00", run.get("loaded_tran_cat_bal_sum"), "loaded_tran_cat_bal_sum");
        assertAmount("120.50", run.get("loaded_dis_int_rate_sum"), "loaded_dis_int_rate_sum");
        // RC2, the other sums: limits 5000.00 + 6000.00 and 2500.00 + 3000.00; cycle credit 1000.00 + 2400.00; debit 0.
        assertAmount("11000.00", run.get("loaded_acct_credit_limit_sum"), "loaded_acct_credit_limit_sum");
        assertAmount("5500.00", run.get("loaded_acct_cash_credit_limit_sum"), "loaded_acct_cash_credit_limit_sum");
        assertAmount("3400.00", run.get("loaded_acct_curr_cyc_credit_sum"), "loaded_acct_curr_cyc_credit_sum");
        assertAmount("0.00", run.get("loaded_acct_curr_cyc_debit_sum"), "loaded_acct_curr_cyc_debit_sum");
        // Hash totals: 2001 + 2002 in each of the three files that carry an account id.
        assertAmount("4003", run.get("loaded_acct_id_sum"), "loaded_acct_id_sum");
        assertAmount("4003", run.get("loaded_xref_acct_id_sum"), "loaded_xref_acct_id_sum");
        assertAmount("4003", run.get("loaded_trancat_acct_id_sum"), "loaded_trancat_acct_id_sum");
        // RC3, RC4.
        assertEquals(0L, run.get("loaded_g02_difference_count"));
        assertEquals(0L, run.get("loaded_g03_difference_count"));
        assertEquals(2L, run.get("loaded_account_key_count"));
        assertEquals(2L, run.get("loaded_card_xref_key_count"));
        assertEquals(2L, run.get("loaded_tran_cat_balance_key_count"));
        assertEquals(15L, run.get("loaded_disclosure_group_key_count"));
        assertFalse(Files.exists(temp.resolve("actual")), "the load writes no output directory");
    }

    @Test
    void sevenCases_figuresEqualTheStrategyTableAndRowsEncodeBackToTheInput() {
        ReplicaTableWriter tables = new ReplicaTableWriter(jdbc);
        for (Expected expected : STRATEGY_TABLE) {
            LoadReport report = load(Fixtures.input(expected.caseName()));
            Map<String, Object> run = run(report.runId());

            // Final-revision evidence of the ticket: the figures as interest_run holds them.
            System.out.printf("RECONCILIATION %s accounts=%s cards=%s balances=%s rates=%s sumAcctCurrBal=%s sumTranCatBal=%s"
                    + " sumDisIntRate=%s keys=%s/%s/%s/%s hashTotals=%s/%s/%s g02=%s g03=%s status=%s%n", expected.caseName(),
                    run.get("loaded_account_count"), run.get("loaded_card_xref_count"), run.get("loaded_tran_cat_balance_count"),
                    run.get("loaded_disclosure_group_count"), run.get("loaded_acct_curr_bal_sum"), run.get("loaded_tran_cat_bal_sum"),
                    run.get("loaded_dis_int_rate_sum"), run.get("loaded_account_key_count"), run.get("loaded_card_xref_key_count"),
                    run.get("loaded_tran_cat_balance_key_count"), run.get("loaded_disclosure_group_key_count"),
                    run.get("loaded_acct_id_sum"), run.get("loaded_xref_acct_id_sum"), run.get("loaded_trancat_acct_id_sum"),
                    run.get("loaded_g02_difference_count"), run.get("loaded_g03_difference_count"), run.get("status"));
            System.out.printf("TL4-PROFILE %s findings=%d %s%n", expected.caseName(), report.profile().findings(), report.profile());

            String caseName = expected.caseName();
            assertEquals(expected.accounts(), run.get("loaded_account_count"), caseName);
            assertEquals(expected.cards(), run.get("loaded_card_xref_count"), caseName);
            assertEquals(expected.balances(), run.get("loaded_tran_cat_balance_count"), caseName);
            assertEquals(expected.rates(), run.get("loaded_disclosure_group_count"), caseName);
            assertAmount(expected.currentBalanceSum(), run.get("loaded_acct_curr_bal_sum"), caseName);
            assertAmount(expected.categoryBalanceSum(), run.get("loaded_tran_cat_bal_sum"), caseName);
            assertAmount(expected.rateSum(), run.get("loaded_dis_int_rate_sum"), caseName);
            assertEquals(expected.accounts(), count("account"), caseName);
            assertEquals(expected.cards(), count("card_xref"), caseName);
            assertEquals(expected.balances(), count("tran_cat_balance"), caseName);
            assertEquals(expected.rates(), count("disclosure_group"), caseName);
            assertAmount(expected.currentBalanceSum(), sum("account", "acct_curr_bal"), caseName);
            assertAmount(expected.categoryBalanceSum(), sum("tran_cat_balance", "tran_cat_bal"), caseName);
            assertAmount(expected.rateSum(), sum("disclosure_group", "dis_int_rate"), caseName);
            assertEquals(0L, run.get("loaded_g02_difference_count"), caseName);
            assertEquals(0L, run.get("loaded_g03_difference_count"), caseName);
            assertEquals("LOADED", run.get("status"), caseName);

            // RC3: every row of every table, encoded again, is its input record (the files are in key order).
            for (ExtractFile file : ExtractFile.values()) {
                RecordCodec codec = new RecordCodec(file.layout(), CodePage.fixture());
                List<String> encoded = new ArrayList<>();
                for (RecordValues row : tables.rowsInKeyOrder(file)) {
                    encoded.add(RecordImage.toLine(codec.encode(row, new RecordSource(file.table(), encoded.size() + 1)), CodePage.fixture()));
                }
                assertEquals(LoadFixtures.lines(caseName, file), encoded, caseName + " " + file.table());
            }
            // TL4 on the fixtures: nothing found.
            assertEquals(0, report.profile().findings(), caseName + " " + report.profile());
        }
    }

    @Test
    void loadingTwice_leavesTheSameRows() {
        load(Fixtures.input("i05-multi-category"));
        List<String> afterFirst = tableContents();
        load(Fixtures.input("i05-multi-category"));

        assertEquals(afterFirst, tableContents());
        assertEquals(1 + 1 + 2 + 15, afterFirst.size());
    }

    @Test
    void loadReplacesWhatAnEarlierCaseLeft() {
        load(Fixtures.input("i01-baseline"));
        load(Fixtures.input("i06-negative-balance"));

        assertEquals(1, count("account"));
        assertEquals(List.of(new BigDecimal("2008")), jdbc.queryForList("select acct_id from carddemo.account", BigDecimal.class));
    }

    @Test
    void undecodableRecord_stepFailsTablesAreUntouchedRejectListIsWritten() throws Exception {
        load(Fixtures.input("i01-baseline"));
        List<String> before = tableContents();
        // i07 with a letter inside TRAN-CAT-BAL (bytes 17-27) of the second balance record.
        Path bad = LoadFixtures.copyWithLine("i07-nontie-rounding", temp.resolve("bad"), ExtractFile.TRAN_CAT_BALANCE, 2,
                line -> LoadFixtures.withCharAt(line, 20, 'X'));

        JobExecution execution = launchStep(bad);

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        Throwable failure = execution.getStepExecutions().iterator().next().getFailureExceptions().get(0);
        assertEquals(InterestRunStoppedException.class, failure.getClass());
        assertEquals(StopReason.RECORD_REJECTED, ((InterestRunStoppedException) failure).reason());
        assertEquals(before, tableContents(), "the four tables hold what they held before");
        assertEquals("FAILED", run(lastRunId()).get("status"));
        assertTrue(run(lastRunId()).get("finished_at") != null);
        assertEquals(null, run(lastRunId()).get("loaded_account_count"));
        assertEquals(List.of("AWS.M2.CARDDEMO.TCATBALF.PS.txt\t2\tTRAN-CAT-BAL\tnot numeric (E2)"),
                Files.readAllLines(rejectFile()));
        assertFalse(execution.getExecutionContext().containsKey(LoadExtractsStepConfig.RUN_ID_KEY));

        // The next good load removes the reject list of the failed attempt.
        assertEquals(BatchStatus.COMPLETED, launchStep(Fixtures.input("i07-nontie-rounding")).getStatus());
        assertFalse(Files.exists(rejectFile()));
    }

    @Test
    void missingFile_stepFailsBeforeAnyTableIsTouched() {
        load(Fixtures.input("i02-tie-half-cent"));
        List<String> before = tableContents();
        Path incomplete = LoadFixtures.copyWithLine("i01-baseline", temp.resolve("incomplete"), ExtractFile.ACCOUNT, 1, line -> line);
        assertTrue(incomplete.resolve(LoadFixtures.fileName(ExtractFile.DISCLOSURE_GROUP)).toFile().delete());

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class, () -> load(incomplete));

        assertEquals(StopReason.OPEN_INPUT, stopped.reason());
        assertEquals(before, tableContents());
        assertEquals("FAILED", run(lastRunId()).get("status"));
    }

    @Test
    void duplicateKey_isRejectedBeforeAnyTableIsTouched() {
        load(Fixtures.input("i01-baseline"));
        List<String> before = tableContents();
        Path twin = LoadFixtures.copyWithExtraLine("i01-baseline", temp.resolve("twin"), ExtractFile.DISCLOSURE_GROUP,
                "DEFAULT   01000100210{");

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class, () -> load(twin));

        assertEquals(StopReason.RECORD_REJECTED, stopped.reason());
        assertEquals(before, tableContents());
        assertEquals("FAILED", run(lastRunId()).get("status"));
    }

    @Test
    void databaseRefusesARow_thatTablesLoadIsRolledBackAndTheRunIsFailed() {
        load(Fixtures.input("i06-negative-balance"));
        List<String> balancesBefore = jdbc.queryForList("select t::text from carddemo.tran_cat_balance t", String.class);
        // Test-only constraint in this container, so that the database refuses the 2400.00 balance of i01 inside
        // the transaction of tran_cat_balance, after TRUNCATE and the first INSERT of the batch.
        jdbc.execute("alter table carddemo.tran_cat_balance add constraint it_only check (tran_cat_bal < 2000) not valid");
        try {
            InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                    () -> load(Fixtures.input("i01-baseline")));

            assertEquals(StopReason.WRITE_OUTPUT, stopped.reason());
            assertEquals(balancesBefore, jdbc.queryForList("select t::text from carddemo.tran_cat_balance t", String.class),
                    "TRUNCATE and the INSERTs of the failed table are rolled back together");
            Map<String, Object> run = run(lastRunId());
            assertEquals("FAILED", run.get("status"));
            // One transaction per table (architecture.md section 5): the two tables before it are already replaced.
            assertEquals(2L, run.get("loaded_account_count"));
            assertEquals(2L, run.get("loaded_card_xref_count"));
            assertEquals(null, run.get("loaded_tran_cat_balance_count"));
            assertEquals(null, run.get("loaded_disclosure_group_count"));
            assertEquals(2, count("account"));
        } finally {
            jdbc.execute("alter table carddemo.tran_cat_balance drop constraint it_only");
        }
    }

    @Test
    void g02AndG03_differencesAreCountedInInterestRunAndInTheProfile() {
        // F-signed balance in TCATBALF record 1 (last byte of TRAN-CAT-BAL, offset 27: '{' -> '0'): same value 1000.00.
        Path fSigned = LoadFixtures.copyWithLine("i01-baseline", temp.resolve("fsigned"), ExtractFile.TRAN_CAT_BALANCE, 1,
                line -> LoadFixtures.withCharAt(line, 27, '0'));
        LoadReport report = load(fSigned);

        assertEquals(1L, run(report.runId()).get("loaded_g02_difference_count"));
        assertEquals(0L, run(report.runId()).get("loaded_g03_difference_count"));
        assertAmount("3400.00", run(report.runId()).get("loaded_tran_cat_bal_sum"), "an F-signed amount keeps its value");
        assertEquals(1, report.profile().fSignedAmounts());

        // Minus zero in DISCGRP record 3 (rate 0000.00, offset 21: '{' -> '}').
        Path minusZero = LoadFixtures.copyWithLine("i01-baseline", temp.resolve("minuszero"), ExtractFile.DISCLOSURE_GROUP, 3,
                line -> LoadFixtures.withCharAt(line, 21, '}'));
        report = load(minusZero);

        assertEquals(0L, run(report.runId()).get("loaded_g02_difference_count"));
        assertEquals(1L, run(report.runId()).get("loaded_g03_difference_count"));
        assertEquals(1, report.profile().minusZeroAmounts());
        assertEquals(1, report.profile().findings());
    }

    @Test
    void tl4_profileFindsSeveralCardsInactiveAccountsMixedCodesAndBalancesWithoutRate() {
        // A second card for account 2001 (G08): card 4000000000002099, customer 000002001, account 00000002001.
        Path twoCards = LoadFixtures.copyWithExtraLine("i01-baseline", temp.resolve("twocards"), ExtractFile.CARD_XREF,
                "400000000000209900000200100000002001");
        assertEquals(new ExtractProfile(1, 0, 0, 0, 0, 0, 0), load(twoCards).profile());

        // Account 2001 not active (INT-H04): ACCT-ACTIVE-STATUS is byte 11.
        Path inactive = LoadFixtures.copyWithLine("i01-baseline", temp.resolve("inactive"), ExtractFile.ACCOUNT, 1,
                line -> LoadFixtures.withCharAt(line, 11, 'N'));
        assertEquals(new ExtractProfile(0, 0, 0, 0, 0, 1, 0), load(inactive).profile());

        // A second balance of account 2001 with type code 'AB' and category 0001 (G06): mixed with '01', and no
        // rate exists for type 'AB' in GOLD or DEFAULT (INT-R08).
        Path mixed = LoadFixtures.copyWithExtraLine("i01-baseline", temp.resolve("mixed"), ExtractFile.TRAN_CAT_BALANCE,
                "00000002001AB00010000010000{");
        assertEquals(new ExtractProfile(0, 1, 0, 0, 0, 0, 1), load(mixed).profile());
    }

    @Test
    void rel08_hostFormatLoadsTheSameRowsAsTheFixtureFormat() throws Exception {
        load(Fixtures.input("i01-baseline"));
        List<String> fromFixtureFormat = tableContents();
        // The same four files as a binary unload (TL2): cp037, C/D signs, no separators.
        Path host = Files.createDirectories(temp.resolve("host"));
        for (ExtractFile file : ExtractFile.values()) {
            RecordCodec fixtureCodec = new RecordCodec(file.layout(), CodePage.fixture());
            RecordCodec hostCodec = new RecordCodec(file.layout(), CodePage.host());
            ByteArrayOutputStream block = new ByteArrayOutputStream();
            int recordNumber = 0;
            for (String line : LoadFixtures.lines("i01-baseline", file)) {
                RecordSource source = new RecordSource(file.table(), ++recordNumber);
                block.write(hostCodec.encode(fixtureCodec.decode(
                        RecordImage.fromLine(line, file.layout().length(), CodePage.fixture()), source), source));
            }
            Files.write(host.resolve(file.fileName(ExtractFormat.HOST)), block.toByteArray());
        }

        LoadReport report = loader.load(new LoadRequest(Fixtures.PARM_DATE, host, ExtractFormat.HOST, rejectFile()));

        assertEquals(2, report.figures().get(ExtractFile.ACCOUNT).rowCount());
        // The fillers are raw bytes (E4) and were carried as they are in the fixture, so every row is equal.
        assertEquals(fromFixtureFormat, tableContents());
        assertEquals("LOADED", run(report.runId()).get("status"));
    }
}
