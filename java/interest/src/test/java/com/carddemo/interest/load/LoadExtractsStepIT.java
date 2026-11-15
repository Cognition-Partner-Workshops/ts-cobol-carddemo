package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * loadExtractsStep on PostgreSQL 15 in a container with the schema of FI-4 (standards S7.3): the
 * same image and init scripts as the db service of docker-compose.yml. Expected figures are the
 * reconciliation table of strategy-interest.md, typed by hand in LoadFixtures (standards S7.4).
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class LoadExtractsStepIT {

    private static final String CASE = "i01-baseline";
    private static final Path SCHEMA = Path.of("..", "..", "db", "postgres");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("carddemo")
            .withUsername("carddemo")
            .withPassword("carddemo")
            .withCopyFileToContainer(MountableFile.forHostPath(SCHEMA.resolve("01-transaction-type.sql")),
                    "/docker-entrypoint-initdb.d/01-transaction-type.sql")
            .withCopyFileToContainer(MountableFile.forHostPath(SCHEMA.resolve("02-interest.sql")),
                    "/docker-entrypoint-initdb.d/02-interest.sql");

    /** The step alone: interestCalcJob is wired by the tickets that own the batch package. */
    @TestConfiguration
    static class LoadOnlyJob {
        @Bean
        Job loadOnlyJob(JobRepository jobRepository, @Qualifier("loadExtractsStep") Step loadExtractsStep) {
            return new JobBuilder("loadOnlyJob", jobRepository).start(loadExtractsStep).build();
        }
    }

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    @Qualifier("loadOnlyJob")
    private Job loadOnlyJob;
    @Autowired
    private ExtractLoader loader;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    private JobExecution launch(Path inputDir) throws Exception {
        return jobLauncher.run(loadOnlyJob, new JobParametersBuilder()
                .addString("parmDate", Fixtures.PARM_DATE)
                .addString("runId", UUID.randomUUID().toString())
                .addString("inputDir", inputDir.toString())
                .addString("outputDir", temp.resolve("out").toString())
                .addString("format", "fixture")
                .toJobParameters());
    }

    private long count(ExtractFile file) {
        return jdbc.queryForObject("SELECT count(*) FROM " + file.table(), Long.class);
    }

    private BigDecimal sum(ExtractFile file, String column) {
        return jdbc.queryForObject("SELECT sum(" + column + ") FROM " + file.table(), BigDecimal.class);
    }

    private Map<String, Object> run(long runId) {
        return jdbc.queryForMap("SELECT * FROM carddemo.interest_run WHERE run_id = ?", runId);
    }

    private Map<String, Object> lastRun() {
        return jdbc.queryForMap("SELECT * FROM carddemo.interest_run ORDER BY run_id DESC LIMIT 1");
    }

    /** RC3: every row of the table encoded again; the FILLER bytes are the column as it is (E4). */
    private List<String> encodedRows(ExtractFile file) {
        RecordCodec codec = RecordFormat.fixture().codec(file.layout());
        List<FieldLayout> fields = file.layout().fields();
        return jdbc.query("SELECT " + String.join(", ", file.columns()) + " FROM " + file.table(), (row, n) -> {
            RecordValues values = RecordValues.blank(file.layout());
            for (int index = 0; index < fields.size(); index++) {
                FieldLayout field = fields.get(index);
                values = switch (field.kind()) {
                    case UNSIGNED -> values.with(field.name(), row.getLong(index + 1));
                    case SIGNED -> values.with(field.name(), row.getBigDecimal(index + 1));
                    case TEXT -> values.with(field.name(), row.getString(index + 1).stripTrailing());
                    case FILLER, REFUSED -> values;
                };
            }
            byte[] record = codec.encode(values, RecordOrigin.IN_MEMORY);
            for (int index = 0; index < fields.size(); index++) {
                if (fields.get(index).kind() == com.carddemo.interest.codec.RecordLayout.Kind.FILLER) {
                    byte[] filler = row.getBytes(index + 1);
                    System.arraycopy(filler, 0, record, fields.get(index).offset(), filler.length);
                }
            }
            return new String(record, StandardCharsets.ISO_8859_1);
        });
    }

    private static List<String> sourceRecords(String caseName, ExtractFile file) {
        return RecordFormat.fixture().records(LoadFixtures.content(caseName, file), file.layout(), file.fileName())
                .stream().map(record -> new String(record, StandardCharsets.ISO_8859_1)).toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.carddemo.interest.load.LoadFixtures#reconciliationTable")
    void everyCase_tablesAndInterestRunHoldTheReconciliationTable(String caseName, long accounts, long cards,
            long balances, long rates, String accountBalance, String categoryBalance, String rate) throws Exception {
        JobExecution execution = launch(Fixtures.input(caseName));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        // RC1
        assertEquals(accounts, count(ExtractFile.ACCOUNTS));
        assertEquals(cards, count(ExtractFile.CARD_XREFS));
        assertEquals(balances, count(ExtractFile.CATEGORY_BALANCES));
        assertEquals(rates, count(ExtractFile.DISCLOSURE_GROUPS));
        // RC2
        assertEquals(LoadFixtures.amount(accountBalance), sum(ExtractFile.ACCOUNTS, "acct_curr_bal"));
        assertEquals(LoadFixtures.amount(categoryBalance), sum(ExtractFile.CATEGORY_BALANCES, "tran_cat_bal"));
        assertEquals(LoadFixtures.amount(rate), sum(ExtractFile.DISCLOSURE_GROUPS, "dis_int_rate"));
        // the same figures in interest_run, the row the step names in the job execution context
        Map<String, Object> run = run(execution.getExecutionContext().getLong(LoadExtractsStepConfig.RUN_ID_KEY));
        assertEquals("LOADED", run.get("status"));
        assertEquals(Fixtures.PARM_DATE, run.get("parm_date"));
        assertEquals(accounts, run.get("loaded_account_count"));
        assertEquals(cards, run.get("loaded_card_xref_count"));
        assertEquals(balances, run.get("loaded_tran_cat_balance_count"));
        assertEquals(rates, run.get("loaded_disclosure_group_count"));
        assertEquals(LoadFixtures.amount(accountBalance), run.get("loaded_acct_curr_bal_sum"));
        assertEquals(LoadFixtures.amount(categoryBalance), run.get("loaded_tran_cat_bal_sum"));
        assertEquals(LoadFixtures.amount(rate), run.get("loaded_dis_int_rate_sum"));
        // RC3: byte for byte, no G02 or G03 difference in the fixtures
        for (ExtractFile file : ExtractFile.values()) {
            List<String> source = new ArrayList<>(sourceRecords(caseName, file));
            List<String> encoded = new ArrayList<>(encodedRows(file));
            source.sort(null);
            encoded.sort(null);
            assertEquals(source, encoded, file.fileName());
        }
        System.out.println("reconciliation " + caseName + ": account " + count(ExtractFile.ACCOUNTS)
                + ", card_xref " + count(ExtractFile.CARD_XREFS) + ", tran_cat_balance " + count(ExtractFile.CATEGORY_BALANCES)
                + ", disclosure_group " + count(ExtractFile.DISCLOSURE_GROUPS)
                + ", SUM(acct_curr_bal) " + sum(ExtractFile.ACCOUNTS, "acct_curr_bal")
                + ", SUM(tran_cat_bal) " + sum(ExtractFile.CATEGORY_BALANCES, "tran_cat_bal")
                + ", SUM(dis_int_rate) " + sum(ExtractFile.DISCLOSURE_GROUPS, "dis_int_rate")
                + ", interest_run " + run.get("status") + " " + run.get("loaded_account_count") + "/"
                + run.get("loaded_card_xref_count") + "/" + run.get("loaded_tran_cat_balance_count") + "/"
                + run.get("loaded_disclosure_group_count") + " " + run.get("loaded_acct_curr_bal_sum") + " "
                + run.get("loaded_tran_cat_bal_sum") + " " + run.get("loaded_dis_int_rate_sum")
                + ", RC3 rows encoded again equal the input records");
    }

    @Test
    void baseline_everyLoadedFigureOfInterestRun() {
        LoadOutcome outcome = loader.load(Fixtures.input(CASE), RecordFormat.fixture(), Fixtures.PARM_DATE);

        assertTrue(outcome.isLoaded());
        Map<String, Object> run = run(outcome.runId());
        // credit limits 5000.00 + 6000.00, cash limits 2500.00 + 3000.00, cycle credits 1000.00 + 2400.00, debits 0.00 + 0.00
        assertEquals(new BigDecimal("11000.00"), run.get("loaded_acct_credit_limit_sum"));
        assertEquals(new BigDecimal("5500.00"), run.get("loaded_acct_cash_credit_limit_sum"));
        assertEquals(new BigDecimal("3400.00"), run.get("loaded_acct_curr_cyc_credit_sum"));
        assertEquals(new BigDecimal("0.00"), run.get("loaded_acct_curr_cyc_debit_sum"));
        // hash totals: accounts 2001 + 2002 in each of the three files
        assertEquals(new BigDecimal("4003.00"), run.get("loaded_acct_id_sum"));
        assertEquals(new BigDecimal("4003.00"), run.get("loaded_xref_acct_id_sum"));
        assertEquals(new BigDecimal("4003.00"), run.get("loaded_trancat_acct_id_sum"));
        assertEquals(null, run.get("written_transaction_count"));
        // RC4: lowest and highest key of each table are the first and last record of its file
        for (ExtractFile file : ExtractFile.values()) {
            assertTrue(outcome.table(file).isKeyRangeOfFile(), file.fileName());
        }
        assertTrue(outcome.profile().isClean(), outcome.profile().toString());
    }

    @Test
    void loadingTwice_leavesTheSameRows() throws Exception {
        assertEquals(BatchStatus.COMPLETED, launch(Fixtures.input(CASE)).getStatus());
        List<List<String>> first = new ArrayList<>();
        for (ExtractFile file : ExtractFile.values()) {
            first.add(encodedRows(file).stream().sorted().toList());
        }

        assertEquals(BatchStatus.COMPLETED, launch(Fixtures.input(CASE)).getStatus());

        List<List<String>> second = new ArrayList<>();
        for (ExtractFile file : ExtractFile.values()) {
            second.add(encodedRows(file).stream().sorted().toList());
        }
        assertEquals(first, second);
        assertEquals(2, count(ExtractFile.ACCOUNTS));
        assertEquals(15, count(ExtractFile.DISCLOSURE_GROUPS));
    }

    @Test
    void undecodableRecord_changesNoTableWritesTheRejectListAndEndsFailed() throws Exception {
        assertEquals(BatchStatus.COMPLETED, launch(Fixtures.input(CASE)).getStatus());
        // i05 has 1 account and other balances than i01; byte 17 is the first digit of TRAN-CAT-BAL of CVTRA01Y
        Path bad = LoadFixtures.copyWith("i05-multi-category", temp.resolve("bad"), ExtractFile.CATEGORY_BALANCES, 2,
                LoadFixtures.charAt(17, 'X'));

        JobExecution execution = launch(bad);

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(BatchStatus.FAILED, execution.getStepExecutions().iterator().next().getStatus());
        Throwable failure = execution.getAllFailureExceptions().get(0);
        assertEquals(StopReason.RECORD_REJECTED, ((InterestRunStoppedException) failure).reason());
        // the four tables hold what they held before: the baseline
        for (ExtractFile file : ExtractFile.values()) {
            assertEquals(sourceRecords(CASE, file).stream().sorted().toList(), encodedRows(file).stream().sorted().toList());
        }
        Map<String, Object> run = lastRun();
        assertEquals("FAILED", run.get("status"));
        assertEquals(null, run.get("loaded_account_count"));
        assertEquals(run.get("run_id"), execution.getExecutionContext().getLong(LoadExtractsStepConfig.RUN_ID_KEY));
        List<String> rejects = Files.readAllLines(temp.resolve("out").resolve(LoadExtractsStepConfig.REJECT_LIST));
        assertEquals(1, rejects.size());
        assertArrayEquals(new String[] {"AWS.M2.CARDDEMO.TCATBALF.PS.txt", "2", "TRAN-CAT-BAL"},
                List.of(rejects.get(0).split("\t")).subList(0, 3).toArray());
    }

    @Test
    void failedInsert_rollsBackThatTable() {
        assertTrue(loader.load(Fixtures.input(CASE), RecordFormat.fixture(), Fixtures.PARM_DATE).isLoaded());
        // i07 holds balances above 1.00: with this constraint the INSERTs of tran_cat_balance fail after its TRUNCATE
        jdbc.execute("ALTER TABLE carddemo.tran_cat_balance ADD CONSTRAINT load_it_refuses CHECK (tran_cat_bal < 1) NOT VALID");
        try {
            InterestRunStoppedException stopped = assertThrows(InterestRunStoppedException.class,
                    () -> loader.load(Fixtures.input("i07-nontie-rounding"), RecordFormat.fixture(), Fixtures.PARM_DATE));
            assertEquals(StopReason.WRITE_OUTPUT, stopped.reason());
        } finally {
            jdbc.execute("ALTER TABLE carddemo.tran_cat_balance DROP CONSTRAINT load_it_refuses");
        }
        // the transaction of tran_cat_balance was rolled back: the table holds the baseline, not an empty table
        assertEquals(sourceRecords(CASE, ExtractFile.CATEGORY_BALANCES).stream().sorted().toList(),
                encodedRows(ExtractFile.CATEGORY_BALANCES).stream().sorted().toList());
        assertEquals("FAILED", lastRun().get("status"));
    }

    @Test
    void missingFile_stopsTheRunBeforeAnyTableAndEndsFailed() throws Exception {
        assertTrue(loader.load(Fixtures.input(CASE), RecordFormat.fixture(), Fixtures.PARM_DATE).isLoaded());
        Path incomplete = LoadFixtures.copyWith(CASE, temp.resolve("incomplete"), null, 0, line -> line);
        Files.delete(incomplete.resolve(ExtractFile.DISCLOSURE_GROUPS.fileName()));

        JobExecution execution = launch(incomplete);

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(StopReason.OPEN_INPUT, ((InterestRunStoppedException) execution.getAllFailureExceptions().get(0)).reason());
        assertEquals(15, count(ExtractFile.DISCLOSURE_GROUPS));
        assertEquals("FAILED", lastRun().get("status"));
        assertFalse(Files.exists(temp.resolve("out").resolve(LoadExtractsStepConfig.REJECT_LIST)));
    }
}
