package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestTransaction;

/**
 * INT-H04 option B through the job: interestCalcJob with interest.policy.inactiveAccounts=SKIP on
 * PostgreSQL 15 in a container (standards S7.3). The active flag reaches the calculation through
 * the file adapter.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK,
        "interest.policy.inactiveAccounts=SKIP"})
@Testcontainers
class InactiveAccountsSkipJobIT {

    private static final String CASE = "i01-baseline";
    private static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    private static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    private static final String ACCOUNTS_INPUT = "AWS.M2.CARDDEMO.ACCTDATA.PS.txt";

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
    void intH04_skipChangesNothingInTheCases(String caseName) throws Exception {
        // every fixture account is active, so option B must give the recorded legacy run, byte for byte
        Path output = temp.resolve("actual");

        JobExecution execution = launch(Fixtures.input(caseName), output);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        for (String file : List.of(TRANSACTIONS, ACCOUNTS, RUN_LOG, "rc.json")) {
            assertEquals(Fixtures.text(Fixtures.expected(caseName).resolve(file)), Fixtures.text(output.resolve(file)),
                    caseName + " " + file);
        }
    }

    /** The i01-baseline input with the active flag of its first account (2001) set to N. */
    private Path i01WithAccount2001Inactive() throws Exception {
        Path input = temp.resolve("input");
        Files.createDirectories(input);
        try (var files = Files.list(Fixtures.input(CASE))) {
            for (Path file : files.toList()) {
                Files.copy(file, input.resolve(file.getFileName()));
            }
        }
        List<String> accounts = Fixtures.lines(input.resolve(ACCOUNTS_INPUT)).stream()
                .map(line -> line.startsWith("00000002001Y") ? "00000002001N" + line.substring(12) : line)
                .toList();
        assertEquals(1, accounts.stream().filter(line -> line.startsWith("00000002001N")).count());
        Files.writeString(input.resolve(ACCOUNTS_INPUT), String.join("\n", accounts) + "\n", StandardCharsets.ISO_8859_1);
        return input;
    }

    @Test
    void intH04_inactiveAccountSkippedThroughTheJob() throws Exception {
        // no fixture has an inactive account. Recorded i01: account 2001 gets transaction ...000001
        // (10.00) and is updated at the break; 2002 gets ...000002 (37.00) and, ending the input, is
        // not updated (INT-Q04). With 2001 inactive and SKIP: 2001 gets nothing and keeps its record,
        // and the transaction of 2002 is the first of the run: 2400.00 * 18.50 / 1200 = 37.00.
        Path input = i01WithAccount2001Inactive();
        Path output = temp.resolve("actual");

        JobExecution execution = launch(input, output);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        List<InterestTransaction> written = Fixtures.lines(output.resolve(TRANSACTIONS)).stream()
                .map(InactiveAccountsSkipJobIT::transaction).toList();
        assertEquals(1, written.size());
        assertEquals("2022071800000001", written.get(0).id());
        assertEquals("Int. for a/c 00000002002", written.get(0).description());
        assertEquals(new BigDecimal("37.00"), written.get(0).amount());
        // no account record changes: 2001 is skipped, 2002 ends the input
        assertEquals(Fixtures.lines(input.resolve(ACCOUNTS_INPUT)).stream().map(String::stripTrailing).toList(),
                Fixtures.lines(output.resolve(ACCOUNTS)).stream().map(String::stripTrailing).toList());
        // the balance records are still echoed as read (CBACT04C:193 comes before the account is fetched)
        assertEquals(Fixtures.text(Fixtures.expected(CASE).resolve(RUN_LOG)), Fixtures.text(output.resolve(RUN_LOG)));
    }

    private static InterestTransaction transaction(String line) {
        return TransactionRecord.decode(String.format("%-" + TransactionRecord.LENGTH + "s", line), RecordOrigin.IN_MEMORY);
    }
}
