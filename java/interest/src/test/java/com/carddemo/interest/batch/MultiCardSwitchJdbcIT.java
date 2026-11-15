package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import com.carddemo.interest.jdbc.InterestTables;

/**
 * Ticket 08, INT-H03 with the switch set from configuration: interest.policy.multiCard=FIRST_BY_CARD_NUM.
 * Synthetic data: i01-baseline with a second, lower card for account 2001. What the host does with
 * two cards is not determined by the source (G08); no parity is claimed.
 */
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "interest.calc.store=jdbc",
        "interest.policy.multiCard=FIRST_BY_CARD_NUM",
        "interest.clock.frozen=" + Fixtures.FROZEN_CLOCK})
@Testcontainers
class MultiCardSwitchJdbcIT {

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

    @Test
    void intH03_firstByCardNumberWhenSwitched() throws Exception {
        InterestTables.load(jdbc, Fixtures.input("i01-baseline"));
        InterestTables.insertCard(jdbc, "300000000000900100000200100000002001");

        JobExecution execution = jobLauncher.run(interestCalcJob, new JobParametersBuilder()
                .addString(InterestJobParameters.PARM_DATE, Fixtures.PARM_DATE)
                .addString(InterestJobParameters.RUN_ID, UUID.randomUUID().toString())
                .addString(InterestJobParameters.INPUT_DIR, Fixtures.input("i01-baseline").toString())
                .addString(InterestJobParameters.OUTPUT_DIR, temp.resolve("actual").toString())
                .addString(InterestJobParameters.FORMAT, "fixture")
                .toJobParameters());

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        // account 2001 gets the lower of its two card numbers; account 2002 its only card
        assertEquals(List.of("3000000000009001", "4000000000002002"), jdbc.queryForList(
                "SELECT tran_card_num FROM carddemo.interest_transaction WHERE run_id = ? ORDER BY seq_no", String.class,
                execution.getExecutionContext().getLong(JdbcCalcInterestRun.INTEREST_RUN_ID)));
    }
}
