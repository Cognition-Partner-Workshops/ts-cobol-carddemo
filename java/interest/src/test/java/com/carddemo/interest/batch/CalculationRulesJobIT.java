package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;

/**
 * Ticket 04 through interestCalcJob with the configured (legacy) policy: each of the seven
 * recorded cases ends COMPLETED with the recorded output, byte for byte, and an empty balance
 * input ends COMPLETED having written nothing (INT-T09). PostgreSQL 15 in a container holds the
 * job repository (standards S7.3).
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class CalculationRulesJobIT {

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

    @TempDir
    Path temp;

    private JobExecution launch(Path inputDir, Path outputDir) throws Exception {
        return jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, inputDir.toString())
                .addString(InterestJobParameters.OUTPUT_DIR, outputDir.toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());
    }

    @ParameterizedTest
    @ValueSource(strings = {"i01-baseline", "i02-tie-half-cent", "i03-zero-rate-skipped", "i04-default-group-fallback",
            "i05-multi-category", "i06-negative-balance", "i07-nontie-rounding"})
    void sevenCases_completeWithTheRecordedOutput(String caseName) throws Exception {
        Path output = temp.resolve("actual");

        JobExecution execution = launch(Fixtures.input(caseName), output);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG, "rc.json")) {
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(output.resolve(file)),
                    caseName + " " + file);
        }
    }

    @Test
    void intT09_emptyInputEndsCompleted() throws Exception {
        // no fixture: an empty balance file with the other inputs of i01-baseline
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        try (var files = Files.list(Fixtures.input("i01-baseline"))) {
            for (Path file : files.toList()) {
                Files.copy(file, input.resolve(file.getFileName()));
            }
        }
        Files.write(input.resolve("AWS.M2.CARDDEMO.TCATBALF.PS.txt"), List.<String>of(), StandardCharsets.ISO_8859_1);
        Path output = temp.resolve("actual");

        JobExecution execution = launch(input, output);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals("", Fixtures.text(output.resolve(TRANSACTIONS)));
        // no account is touched: the image equals the input file
        assertEquals(Fixtures.lines(input.resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).stream().map(String::stripTrailing).toList(),
                Fixtures.lines(output.resolve(ACCOUNTS)).stream().map(String::stripTrailing).toList());
        assertEquals(List.of("START OF EXECUTION OF PROGRAM CBACT04C", "END OF EXECUTION OF PROGRAM CBACT04C"),
                Fixtures.lines(output.resolve(RUN_LOG)));
        assertEquals("0/0", jdbc.queryForObject(
                "SELECT read_count || '/' || write_count FROM batch.batch_step_execution WHERE job_execution_id = ?",
                String.class, execution.getId()));
    }
}
