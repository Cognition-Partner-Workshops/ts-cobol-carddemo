package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;

/**
 * interestCalcJob end to end on PostgreSQL 15 in a container (standards S7.3): the same image and
 * init script as the db service of docker-compose.yml. Outputs are compared with the recorded
 * legacy runs of the seven cases, byte for byte (standards S7.4).
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class InterestCalcJobIT {

    private static final String CASE = "i01-baseline";
    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("carddemo")
            .withUsername("carddemo")
            .withPassword("carddemo")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", "01-transaction-type.sql")),
                    "/docker-entrypoint-initdb.d/01-transaction-type.sql");

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job interestCalcJob;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private InterestExitCodes exitCodes;

    @TempDir
    Path temp;

    private JobExecution launch(Path inputDir, Path outputDir, String runId) throws Exception {
        return jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, runId)
                .addString(InterestJobParameters.INPUT_DIR, inputDir.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    private Path runBaseline() throws Exception {
        Path output = temp.resolve("actual");
        assertEquals(BatchStatus.COMPLETED, launch(Fixtures.input(CASE), output, UUID.randomUUID().toString()).getStatus());
        return output;
    }

    private static void assertSameAsRecorded(Path output, String file) {
        assertEquals(Fixtures.text(Fixtures.expected(CASE).resolve(file)), Fixtures.text(output.resolve(file)), file);
    }

    @Test
    void i01Baseline_datasetsMatchRecorded() throws Exception {
        Path output = runBaseline();

        assertSameAsRecorded(output, TRANSACTIONS);
        assertSameAsRecorded(output, ACCOUNTS);
        assertEquals(2, Fixtures.lines(output.resolve(TRANSACTIONS)).size());
        assertEquals(2, Fixtures.lines(output.resolve(ACCOUNTS)).size());
    }

    @Test
    void sysout_matchesRecorded() throws Exception {
        assertSameAsRecorded(runBaseline(), RUN_LOG);
    }

    @Test
    void rcJson_matchesRecorded() throws Exception {
        assertSameAsRecorded(runBaseline(), "rc.json");
    }

    @Test
    void outputDirectoryHoldsOnlyTheRecordedFiles() throws Exception {
        Path output = runBaseline();

        try (var files = Files.walk(output)) {
            assertEquals(List.of(ACCOUNTS, TRANSACTIONS, "rc.json", RUN_LOG),
                    files.filter(Files::isRegularFile).map(file -> output.relativize(file).toString()).sorted().toList());
        }
        try (var siblings = Files.list(temp)) {
            assertEquals(List.of("actual"), siblings.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void intR07_updatesAccountAtBreak() throws Exception {
        // ticket 01: ACCT-CURR-BAL of 00000002001 becomes 1010.00 = 0000001010 0{ ; cycle credit goes to zero
        String account2001 = Fixtures.lines(runBaseline().resolve(ACCOUNTS)).get(0);

        assertEquals("00000002001", account2001.substring(0, 11));
        assertEquals("00000010100{", account2001.substring(12, 24));
        assertEquals("00000000000{", account2001.substring(78, 90));
        assertEquals("00000000000{", account2001.substring(90, 102));
    }

    @Test
    void intQ04_lastAccountNotUpdated() throws Exception {
        // ticket 01: ACCT-CURR-BAL of 00000002002 stays 2400.00 and the record equals its input record
        String account2002 = Fixtures.lines(runBaseline().resolve(ACCOUNTS)).get(1);

        assertEquals("00000024000{", account2002.substring(12, 24));
        assertEquals(Fixtures.lines(Fixtures.input(CASE).resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).get(1), account2002);
        // ... while its 37.00 transaction is written
        assertEquals("0000000370{", Fixtures.lines(temp.resolve("actual").resolve(TRANSACTIONS)).get(1).substring(132, 143));
    }

    @Test
    void intR05_observableAmountsOfTheTicket() throws Exception {
        // ticket 01: TRAN-AMT 10.00 for account 00000002001 and 37.00 for 00000002002
        List<String> transactions = Fixtures.lines(runBaseline().resolve(TRANSACTIONS));

        assertEquals("Int. for a/c 00000002001", transactions.get(0).substring(32, 56));
        assertEquals("0000000100{", transactions.get(0).substring(132, 143));
        assertEquals("Int. for a/c 00000002002", transactions.get(1).substring(32, 56));
        assertEquals("0000000370{", transactions.get(1).substring(132, 143));
    }

    @Test
    void jobRepository_oneCompletedExecutionWithOneStep() throws Exception {
        String runId = UUID.randomUUID().toString();
        JobExecution execution = launch(Fixtures.input(CASE), temp.resolve("actual"), runId);

        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM batch.batch_job_execution e
                  JOIN batch.batch_job_instance i ON i.job_instance_id = e.job_instance_id
                  JOIN batch.batch_job_execution_params p ON p.job_execution_id = e.job_execution_id
                 WHERE i.job_name = 'interestCalcJob' AND e.status = 'COMPLETED'
                   AND p.parameter_name = 'runId' AND p.parameter_value = ?""", Integer.class, runId));
        assertEquals(List.of("calcInterestStep:COMPLETED"), jdbc.queryForList(
                "SELECT step_name || ':' || status FROM batch.batch_step_execution WHERE job_execution_id = ?",
                String.class, execution.getId()));
        // one item is one account: two accounts read and written, in one commit
        assertEquals("2/2", jdbc.queryForObject(
                "SELECT read_count || '/' || write_count FROM batch.batch_step_execution WHERE job_execution_id = ?",
                String.class, execution.getId()));
    }

    @Test
    void schemaCarddemo_holdsOnlyTheTablesOfTheEstate() throws Exception {
        runBaseline();

        // db/postgres/01-transaction-type.sql creates exactly these two; the job repository is in schema batch
        assertEquals(List.of("transaction_type", "transaction_type_categor"), jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'carddemo' ORDER BY 1", String.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'", Integer.class));
        assertEquals("15", jdbc.queryForObject("SHOW server_version", String.class).split("\\.")[0]);
    }

    @Test
    void intR08_missingInputFileFailsTheJobAndLeavesNoOutput() throws Exception {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        try (var files = Files.list(Fixtures.input(CASE))) {
            for (Path file : files.toList()) {
                if (!file.getFileName().toString().contains("DISCGRP")) {
                    Files.copy(file, input.resolve(file.getFileName()));
                }
            }
        }
        Path output = temp.resolve("actual");

        JobExecution execution = launch(input, output, UUID.randomUUID().toString());

        assertEquals(BatchStatus.FAILED, execution.getStatus());
        assertFalse(Files.exists(output));
        try (var siblings = Files.list(temp)) {
            assertEquals(List.of("input"), siblings.map(path -> path.getFileName().toString()).toList());
        }
        exitCodes.onJobExecution(new JobExecutionEvent(execution));
        assertNotEquals(0, exitCodes.getExitCode());
        assertEquals(InterestExitCode.STOPPED, exitCodes.getExitCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void intT02_everyCaseMatchesTheRecordedRun(String caseName) throws Exception {
        Path output = temp.resolve("actual");

        JobExecution execution = launch(Fixtures.input(caseName), output, UUID.randomUUID().toString());

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG, "rc.json")) {
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(output.resolve(file)),
                    caseName + " " + file);
        }
    }

    @Test
    void intR03_fallbackLinesAreInTheRunLog() throws Exception {
        Path output = temp.resolve("actual");

        launch(Fixtures.input("i04-default-group-fallback"), output, UUID.randomUUID().toString());

        // recorded SYSOUT of i04: start line, the balance record, the two fallback lines, end line
        assertEquals(List.of("DISCLOSURE GROUP RECORD MISSING", "TRY WITH DEFAULT GROUP CODE"),
                Fixtures.lines(output.resolve(RUN_LOG)).subList(2, 4));
    }

    @Test
    void intT09_emptyInputWritesNothing() throws Exception {
        // no fixture: the i01-baseline input with an empty balance file
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        try (var files = Files.list(Fixtures.input(CASE))) {
            for (Path file : files.toList()) {
                Files.copy(file, input.resolve(file.getFileName()));
            }
        }
        Files.writeString(input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt"), "");
        Path output = temp.resolve("actual");

        JobExecution execution = launch(input, output, UUID.randomUUID().toString());

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals("", Fixtures.text(output.resolve(TRANSACTIONS)));
        // no account is touched: the account image is the input, record for record
        assertEquals(Fixtures.lines(input.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).stream().map(String::stripTrailing).toList(),
                Fixtures.lines(output.resolve(ACCOUNTS)).stream().map(String::stripTrailing).toList());
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "END OF EXECUTION OF PROGRAM CBACT04C"),
                Fixtures.lines(output.resolve(RUN_LOG)));
    }

    @Test
    void intH02_secondRunWithTheSameParmIsNotRefused() throws Exception {
        // rerunGuard is OFF as coded: a new runId is a new run (architecture.md section 4)
        runBaseline();
        assertEquals(BatchStatus.COMPLETED,
                launch(Fixtures.input(CASE), temp.resolve("actual"), UUID.randomUUID().toString()).getStatus());
        assertSameAsRecorded(temp.resolve("actual"), TRANSACTIONS);
    }
}
