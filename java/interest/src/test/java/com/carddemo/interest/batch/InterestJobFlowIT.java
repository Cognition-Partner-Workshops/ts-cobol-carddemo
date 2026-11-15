package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.load.LoadExtractsStepConfig;

/**
 * Ticket 09: interestCalcJob with the profile {@code database} is loadExtractsStep ->
 * calcInterestStep -> exportDatasetsStep on the tables of db/postgres/02-interest.sql, PostgreSQL 15
 * in a container with the init scripts of the db service (standards S7.3). The input is the
 * unloaded files of a recorded case; the expected side is the recorded legacy output of that case,
 * file by file (standards S7.4). The field-level comparison is the gate itself, make parity-java.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@ActiveProfiles("database")
@Testcontainers
class InterestJobFlowIT {

    private static final List<String> RECORDED_FILES = List.of(
            "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt",
            "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt",
            "sysout/INTCALC.STEP15.txt",
            "rc.json");

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
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path temp;

    private Path outputDir() {
        return temp.resolve("actual");
    }

    private JobParameters parameters(Path inputDir) {
        return new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, inputDir.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir().toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters();
    }

    private static List<String> steps(JobExecution execution) {
        return execution.getStepExecutions().stream()
                .map(step -> step.getStepName() + ":" + step.getStatus())
                .toList();
    }

    private long runCount() {
        return jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_run", Long.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void fixtureCase_threeStepsThroughTheTables_writeTheRecordedOutput(String caseName) throws Exception {
        long runsBefore = runCount();

        JobExecution execution = jobLauncher.run(interestCalcJob, parameters(Fixtures.input(caseName)));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(List.of("loadExtractsStep:COMPLETED", "calcInterestStep:COMPLETED", "exportDatasetsStep:COMPLETED"),
                steps(execution));
        for (String file : RECORDED_FILES) {
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(outputDir().resolve(file)),
                    caseName + " " + file);
        }
        // the three steps work on one row of interest_run: the one the load created
        assertEquals(runsBefore + 1, runCount());
        long loadedRun = execution.getExecutionContext().getLong(LoadExtractsStepConfig.RUN_ID_KEY);
        assertEquals(loadedRun, execution.getExecutionContext().getLong(JdbcCalcInterestRun.INTEREST_RUN_ID));
        assertEquals("EXPORTED",
                jdbc.queryForObject("SELECT status FROM carddemo.interest_run WHERE run_id = ?", String.class, loadedRun));
    }

    @Test
    void i01Baseline_rc1Rc2Rc5_holdInTheRowOfTheRun() throws Exception {
        // i01-baseline input: accounts 2001 (1000.00) and 2002 (2400.00), one card and one balance each.
        // RC1: 2 accounts, 2 cards, 2 balances. RC2: 1000.00 + 2400.00 = 3400.00; ids 2001 + 2002 = 4003.
        // RC5: transactions 10.00 + 37.00 = 47.00; account 2002 ends the input and is not updated (INT-Q04),
        // so one account is updated and the balances add up to 3400.00 + 10.00 = 3410.00 = 3400.00 + 47.00 - 37.00.
        JobExecution execution = jobLauncher.run(interestCalcJob, parameters(Fixtures.input("i01-baseline")));

        long run = execution.getExecutionContext().getLong(LoadExtractsStepConfig.RUN_ID_KEY);
        assertEquals("2/2/2 3400.00/4003.00/4003.00/4003.00 2/47.00/1/3410.00", jdbc.queryForObject("""
                SELECT loaded_account_count || '/' || loaded_card_xref_count || '/' || loaded_tran_cat_balance_count
                       || ' ' || loaded_acct_curr_bal_sum || '/' || loaded_acct_id_sum || '/' || loaded_xref_acct_id_sum
                       || '/' || loaded_trancat_acct_id_sum
                       || ' ' || written_transaction_count || '/' || written_tran_amt_sum || '/' || written_account_count
                       || '/' || written_acct_curr_bal_sum
                  FROM carddemo.interest_run WHERE run_id = ?""", String.class, run));
        // RC1 and RC5 against the tables themselves
        assertEquals("2/2/2/2", jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM carddemo.account) || '/' || (SELECT count(*) FROM carddemo.card_xref)
                       || '/' || (SELECT count(*) FROM carddemo.tran_cat_balance)
                       || '/' || (SELECT count(*) FROM carddemo.interest_transaction WHERE run_id = ?)""", String.class, run));
    }

    @Test
    void aFailedLoad_failsTheJob_noCalculationNoExportNoOutput() throws Exception {
        // INT-R08 / standards S6.2, S6.4: a step that does not complete ends the job; nothing runs after it
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        copyInputWithout(Fixtures.input("i01-baseline"), input, "AWS.M2.CARDDEMO.CARDXREF.PS.txt");
        long transactionsBefore = jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction", Long.class);

        JobExecution execution = jobLauncher.run(interestCalcJob, parameters(input));

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertEquals(List.of("loadExtractsStep:FAILED"), steps(execution));
        assertFalse(Files.exists(outputDir()), "a failed run leaves no output directory");
        assertEquals(transactionsBefore, jdbc.queryForObject("SELECT count(*) FROM carddemo.interest_transaction", Long.class));
    }

    private static void copyInputWithout(Path from, Path to, String leftOut) throws IOException {
        try (var files = Files.list(from)) {
            for (Path file : files.toList()) {
                if (!file.getFileName().toString().equals(leftOut)) {
                    Files.copy(file, to.resolve(file.getFileName()));
                }
            }
        }
    }

    @Test
    void theCalculationStep_isTheSecondOfThree() throws Exception {
        JobExecution execution = jobLauncher.run(interestCalcJob, parameters(Fixtures.input("i05-multi-category")));

        StepExecution calc = execution.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals(InterestJobConfig.CALC_STEP_NAME)).findFirst().orElseThrow();
        // i05-multi-category: one account (2007) with two balances; one item is one account
        assertEquals(1, calc.getReadCount());
        assertEquals(1, calc.getWriteCount());
    }
}
