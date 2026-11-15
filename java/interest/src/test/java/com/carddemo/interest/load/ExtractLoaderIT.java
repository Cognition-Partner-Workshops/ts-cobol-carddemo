package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.batch.InterestJobParameters;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.jdbc.ReplicaTable;
import com.carddemo.interest.jdbc.ReplicaTableReader;

/**
 * INT-T05 on PostgreSQL 15 in a container (standards S7.3), with the init scripts of the db service
 * of docker-compose.yml: db/postgres/01-transaction-type.sql and 02-interest.sql (FI-4).
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class ExtractLoaderIT {

    private static final String BASELINE = "i01-baseline";
    private static final String BALANCES = "AWS.M2.CARDDEMO.TCATBALF.PS.txt";

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
    private ExtractLoader loader;
    @Autowired
    private ReplicaTableReader reader;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    @Qualifier(LoadExtractsStepConfig.STEP_NAME)
    private Step loadExtractsStep;

    @TempDir
    Path temp;

    private LoadResult load(String caseName) {
        return loader.load(Fixtures.input(caseName), ExtractFormat.FIXTURE, Fixtures.PARM_DATE, temp.resolve("rejects.txt"));
    }

    private Map<String, Object> run(long runId) {
        return jdbc.queryForMap("SELECT * FROM carddemo.interest_run WHERE run_id = ?", runId);
    }

    private long lastRunId() {
        return jdbc.queryForObject("SELECT MAX(run_id) FROM carddemo.interest_run", Long.class);
    }

    /** Every row of the four tables as text, in a fixed order. */
    private List<String> tables() {
        List<String> rows = new ArrayList<>();
        for (ReplicaTable table : ReplicaTable.values()) {
            rows.addAll(jdbc.queryForList("SELECT '" + table.name() + " ' || t::text FROM " + table.tableName() + " t ORDER BY 1", String.class));
        }
        return rows;
    }

    private JobExecution launchStep(Path inputDir, Path outputDir) throws Exception {
        Job job = new JobBuilder("loadExtractsStepOnly", jobRepository).start(loadExtractsStep).build();
        return jobLauncher.run(job, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, inputDir.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    @Test
    void i01Baseline_rowsAndSums_inTheTablesAndInInterestRun() {
        LoadResult result = load(BASELINE);

        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM carddemo.account", Long.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM carddemo.card_xref", Long.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM carddemo.tran_cat_balance", Long.class));
        assertEquals(15, jdbc.queryForObject("SELECT COUNT(*) FROM carddemo.disclosure_group", Long.class));
        assertEquals(new BigDecimal("3400.00"), jdbc.queryForObject("SELECT SUM(acct_curr_bal) FROM carddemo.account", BigDecimal.class));
        assertEquals(new BigDecimal("3400.00"), jdbc.queryForObject("SELECT SUM(tran_cat_bal) FROM carddemo.tran_cat_balance", BigDecimal.class));
        assertEquals(new BigDecimal("120.50"), jdbc.queryForObject("SELECT SUM(dis_int_rate) FROM carddemo.disclosure_group", BigDecimal.class));

        Map<String, Object> run = run(result.runId());
        assertEquals("LOADED", run.get("status"));
        assertEquals(Fixtures.PARM_DATE, run.get("parm_date"));
        assertEquals(2L, run.get("loaded_account_count"));
        assertEquals(2L, run.get("loaded_card_xref_count"));
        assertEquals(2L, run.get("loaded_tran_cat_balance_count"));
        assertEquals(15L, run.get("loaded_disclosure_group_count"));
        assertEquals(new BigDecimal("3400.00"), run.get("loaded_acct_curr_bal_sum"));
        assertEquals(new BigDecimal("11000.00"), run.get("loaded_acct_credit_limit_sum"));
        assertEquals(new BigDecimal("5500.00"), run.get("loaded_acct_cash_credit_limit_sum"));
        assertEquals(new BigDecimal("3400.00"), run.get("loaded_acct_curr_cyc_credit_sum"));
        assertEquals(new BigDecimal("0.00"), run.get("loaded_acct_curr_cyc_debit_sum"));
        assertEquals(new BigDecimal("3400.00"), run.get("loaded_tran_cat_bal_sum"));
        assertEquals(new BigDecimal("120.50"), run.get("loaded_dis_int_rate_sum"));
        assertEquals(new BigDecimal("4003.00"), run.get("loaded_acct_id_sum"));
        assertEquals(new BigDecimal("4003.00"), run.get("loaded_xref_acct_id_sum"));
        assertEquals(new BigDecimal("4003.00"), run.get("loaded_trancat_acct_id_sum"));
        assertEquals(null, run.get("written_transaction_count"));
        assertEquals(null, run.get("finished_at"));
    }

    /** The reconciliation table of modernization/design/strategy-interest.md, load side. */
    @ParameterizedTest
    @CsvSource({
            "i01-baseline,               2, 2, 2, 15, 3400.00, 3400.00, 120.50",
            "i02-tie-half-cent,          2, 2, 2, 15, 2470.00, 2470.00, 120.50",
            "i03-zero-rate-skipped,      1, 1, 2, 15,  700.00,  700.00, 120.50",
            "i04-default-group-fallback, 1, 1, 1, 15, 1000.00, 1000.00, 120.50",
            "i05-multi-category,         1, 1, 2, 15, 1500.00, 1500.00, 120.50",
            "i06-negative-balance,       1, 1, 1, 15, -300.00, -300.00, 120.50",
            "i07-nontie-rounding,        2, 2, 2, 15, 1778.10, 1778.10, 120.50"})
    void everyCase_figuresOfTheStrategyTable_andRoundTrip(String caseName, long accounts, long cards, long balances, long rates,
            BigDecimal balanceSum, BigDecimal categoryBalanceSum, BigDecimal rateSum) {
        LoadResult result = load(caseName);
        LoadFigures figures = result.figures();

        System.out.println("reconciliation " + caseName + ": " + figures.describe());
        System.out.println("reconciliation " + caseName + ": RC3 " + result.roundTrip() + " | RC4 key ends equal file ends "
                + result.keyRangeEqualsFileEnds());
        System.out.println("reconciliation " + caseName + ": " + result.profile().describe());

        // RC1, RC2
        assertEquals(accounts, figures.accountCount());
        assertEquals(cards, figures.cardXrefCount());
        assertEquals(balances, figures.tranCatBalanceCount());
        assertEquals(rates, figures.disclosureGroupCount());
        assertEquals(balanceSum, figures.acctCurrBalSum());
        assertEquals(categoryBalanceSum, figures.tranCatBalSum());
        assertEquals(rateSum, figures.disIntRateSum());
        Map<String, Object> run = run(result.runId());
        assertEquals(accounts, run.get("loaded_account_count"));
        assertEquals(balanceSum, run.get("loaded_acct_curr_bal_sum"));
        assertEquals(categoryBalanceSum, run.get("loaded_tran_cat_bal_sum"));
        assertEquals(rateSum, run.get("loaded_dis_int_rate_sum"));

        // RC3: every row, encoded again, is its input record
        long records = accounts + cards + balances + rates;
        assertEquals(new RoundTrip(records, records, 0, 0, 0), result.roundTrip());
        for (ExtractFile file : ExtractFile.values()) {
            List<String> lines = Fixtures.lines(Fixtures.input(caseName).resolve(file.fileName(ExtractFormat.FIXTURE)));
            List<String> rows = reader.records(file.table(), CodePage.FIXTURE).stream()
                    .map(row -> RecordImage.toLine(RecordImage.image(row))).toList();
            assertEquals(lines.stream().sorted().toList(), rows.stream().sorted().toList(), file.name());
        }
        // RC4, TL4
        assertEquals(List.of(true, true, true, true), List.copyOf(result.keyRangeEqualsFileEnds().values()));
        assertTrue(result.profile().nothingFound(), result.profile().describe());
    }

    @Test
    void loadingTwice_leavesTheSameRows() {
        load("i07-nontie-rounding");
        load(BASELINE);
        List<String> once = tables();

        load(BASELINE);

        assertEquals(once, tables());
        assertEquals(21, once.size());
    }

    @Test
    void undecodableRecord_rejectList_noTableChanged_runFailed() throws Exception {
        load("i07-nontie-rounding");
        List<String> before = tables();
        long runsBefore = lastRunId();
        Path input = LoadFiles.copyOf(BASELINE, temp.resolve("in"));
        LoadFiles.edit(input, ExtractFile.BALANCES, lines -> {
            lines.set(1, LoadFiles.overlay(lines.get(1), 20, "X"));
            return lines;
        });
        Path rejectList = temp.resolve("rejects.txt");

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> loader.load(input, ExtractFormat.FIXTURE, Fixtures.PARM_DATE, rejectList));

        assertEquals(StopReason.RECORD_REJECTED, stopped.reason());
        List<String> rejects = Files.readAllLines(rejectList);
        assertEquals(2, rejects.size());
        assertEquals("file\trecord\tfield\treason", rejects.get(0));
        assertTrue(rejects.get(1).startsWith(BALANCES + "\t2\tTRAN-CAT-BAL\t"), rejects.get(1));
        // the accounts and cards of the bad input were decodable, and still nothing was loaded
        assertEquals(before, tables());
        Map<String, Object> run = run(lastRunId());
        assertEquals(runsBefore + 1, run.get("run_id"));
        assertEquals("FAILED", run.get("status"));
        assertEquals(null, run.get("loaded_account_count"));
        assertTrue(run.get("finished_at") != null);
    }

    @Test
    void aGoodLoad_removesAnOlderRejectList() throws Exception {
        Path rejectList = temp.resolve("rejects.txt");
        Files.writeString(rejectList, "old");

        load(BASELINE);

        assertFalse(Files.exists(rejectList));
    }

    @Test
    void missingFile_runFailed_noTableChanged() throws Exception {
        load(BASELINE);
        List<String> before = tables();
        Path input = LoadFiles.copyOf("i07-nontie-rounding", temp.resolve("in"));
        Files.delete(input.resolve("AWS.M2.CARDDEMO.DISCGRP.PS.txt"));

        InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                () -> loader.load(input, ExtractFormat.FIXTURE, Fixtures.PARM_DATE, temp.resolve("rejects.txt")));

        assertEquals(StopReason.OPEN_INPUT, stopped.reason());
        assertEquals(before, tables());
        assertEquals("FAILED", run(lastRunId()).get("status"));
    }

    @Test
    void g02_g03_areCountedAndTheLoadSucceeds() {
        Path input = LoadFiles.copyOf(BASELINE, temp.resolve("in"));
        LoadFiles.edit(input, ExtractFile.BALANCES, lines -> {
            lines.set(0, LoadFiles.overlay(lines.get(0), 17, "00000100000"));  // F-signed 1000.00
            lines.add(LoadFiles.overlay(lines.get(1), 11, "02" + "0001" + "0000000000}")); // minus zero
            return lines;
        });

        LoadResult result = loader.load(input, ExtractFormat.FIXTURE, Fixtures.PARM_DATE, temp.resolve("rejects.txt"));

        assertEquals(new RoundTrip(22, 20, 1, 1, 0), result.roundTrip());
        assertEquals(1, result.profile().fSignedFields());
        assertEquals(1, result.profile().minusZeroFields());
        assertEquals(new BigDecimal("3400.00"), result.figures().tranCatBalSum());
        assertEquals(3, result.figures().tranCatBalanceCount());
    }

    @Test
    void hostFormat_sameRowsAsTheFixtureFormat_andRoundTripOnHostBytes() throws Exception {
        load(BASELINE);
        List<String> fromFixture = tables();
        Path host = LoadFiles.hostCopyOf(BASELINE, temp.resolve("host"));

        LoadResult result = loader.load(host, ExtractFormat.HOST, Fixtures.PARM_DATE, temp.resolve("rejects.txt"));

        assertEquals(fromFixture, tables());
        assertEquals(new RoundTrip(21, 21, 0, 0, 0), result.roundTrip());
        byte[] accounts = Files.readAllBytes(host.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS"));
        List<byte[]> rows = reader.records(ReplicaTable.ACCOUNT, CodePage.host());
        assertArrayEquals(accounts, concat(rows));
        assertEquals(300, rows.get(0).length);
        assertEquals(new BigDecimal("3400.00"), result.figures().acctCurrBalSum());
    }

    @Test
    void step_completes_andLeavesTheRunIdInTheJobContext() throws Exception {
        JobExecution execution = launchStep(Fixtures.input(BASELINE), temp.resolve("actual"));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        long runId = execution.getExecutionContext().getLong(LoadExtractsStepConfig.RUN_ID_KEY);
        assertEquals(lastRunId(), runId);
        assertEquals("LOADED", run(runId).get("status"));
        assertFalse(Files.exists(temp.resolve("actual")));
    }

    @Test
    void step_endsFailed_onAnUndecodableRecord() throws Exception {
        launchStep(Fixtures.input("i07-nontie-rounding"), temp.resolve("actual"));
        List<String> before = tables();
        Path input = LoadFiles.copyOf(BASELINE, temp.resolve("in"));
        LoadFiles.edit(input, ExtractFile.ACCOUNTS, lines -> {
            lines.set(0, LoadFiles.overlay(lines.get(0), 3, "?"));
            return lines;
        });

        JobExecution execution = launchStep(input, temp.resolve("actual"));

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(BatchStatus.FAILED, execution.getStepExecutions().iterator().next().getStatus());
        assertFalse(execution.getExecutionContext().containsKey(LoadExtractsStepConfig.RUN_ID_KEY));
        assertEquals(List.of("file\trecord\tfield\treason"),
                Files.readAllLines(temp.resolve("actual.load-rejects.txt")).subList(0, 1));
        assertTrue(Files.readAllLines(temp.resolve("actual.load-rejects.txt")).get(1)
                .startsWith("AWS.M2.CARDDEMO.ACCTDATA.PS.txt\t1\tACCT-ID\t"));
        assertFalse(Files.exists(temp.resolve("actual")));
        assertEquals(before, tables());
        assertEquals("FAILED", run(lastRunId()).get("status"));
    }

    private static byte[] concat(List<byte[]> records) {
        byte[] all = new byte[records.stream().mapToInt(record -> record.length).sum()];
        int at = 0;
        for (byte[] record : records) {
            System.arraycopy(record, 0, all, at, record.length);
            at += record.length;
        }
        return all;
    }
}
